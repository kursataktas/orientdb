/*
 * Copyright 2016 OrientDB LTD (info(at)orientdb.com)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *   For more information: http://www.orientdb.com
 */

package com.orientechnologies.backup.uploader;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.orientechnologies.agent.services.backup.log.OBackupUploadFinishedLog;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.log.OLogger;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This strategy performs an upload to a S3 bucket. The upload of the delta between the local backup
 * directory and the remote one is performed.
 *
 * @param
 */
public class OS3DeltaUploadingStrategy implements OUploadingStrategy {
  private static final OLogger logger =
      OLogManager.instance().logger(OS3DeltaUploadingStrategy.class);

  private final String suffix = "/";
  private String bucketName;
  private String accessKey;
  private String secretKey;

  public OS3DeltaUploadingStrategy() {}

  //

  /**
   * Uploads a backup to a S3 bucket
   *
   * @param sourceBackupDirectory
   * @param destinationDirectoryPath
   * @param accessParameters (String bucketName, String accessKey, String secretKey)
   * @return success
   */
  public boolean executeUpload(
      String sourceBackupDirectory, String destinationDirectoryPath, String... accessParameters) {

    String bucketName = accessParameters[0];
    String accessKey = accessParameters[1];
    String secretKey = accessParameters[2];

    boolean success = false;
    AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
    AmazonS3Client s3client = new AmazonS3Client(awsCredentials);

    try {
      success =
          executeS3Upload(s3client, bucketName, sourceBackupDirectory, destinationDirectoryPath);
    } catch (AmazonServiceException ase) {
      logger.info(
          "Caught an AmazonServiceException, which "
              + "means your request made it "
              + "to Amazon S3, but was rejected with an error response"
              + " for some reason.");
      logger.info(
          "Error Message:    %s\nHTTP Status Code: %s\nAWS Error Code:   %s\n"
              + "Error Type:       %s\nRequest ID:       %s",
          ase.getMessage(),
          ase.getStatusCode(),
          ase.getErrorCode(),
          ase.getErrorType(),
          ase.getRequestId());
    } catch (AmazonClientException ace) {
      logger.info(
          "Caught an AmazonClientException, which "
              + "means the client encountered "
              + "an internal error while trying to "
              + "communicate with S3, "
              + "such as not being able to access the network.");
      logger.info("Error Message: %s", ace.getMessage());
    } catch (Exception e) {
      logger.info("Caught an exception client side. Error Message: %s", e.getMessage());
    }

    return success;
  }

  protected Boolean executeS3Upload(
      AmazonS3 s3client,
      String bucketName,
      String sourceBackupDirectory,
      String destinationDirectoryPath) {
    /*
     * preparing bucket: if not present it's built
     */

    List<Bucket> buckets = s3client.listBuckets();

    boolean alreadyPresent = false;
    for (Bucket b : buckets) {
      if (b.getName().equals(bucketName)) {
        alreadyPresent = true;
        break;
      }
    }
    // if the bucket is not present build it
    if (!alreadyPresent) {
      s3client.createBucket(bucketName);
    }

    /*
     * uploading file to the bucket
     */

    File localBackupDirectory = new File(sourceBackupDirectory);

    File[] filesLocalBackup = localBackupDirectory.listFiles();
    Map<String, File> localFileName2File = new ConcurrentHashMap<String, File>();
    for (File f : filesLocalBackup) {
      localFileName2File.put(f.getName(), f);
    }

    List<S3ObjectSummary> filesOnBucket = s3client.listObjects(bucketName).getObjectSummaries();
    List<String> remoteFileNames = new ArrayList<String>();

    String currentFileName;
    for (S3ObjectSummary obj : filesOnBucket) {
      remoteFileNames.add(obj.getKey());
    }

    // preparing folder: if folder does not exist  it's created (case: first incremental backup)
    int lastIndex = destinationDirectoryPath.length() - 1;
    if (destinationDirectoryPath.charAt(lastIndex) == '/')
      destinationDirectoryPath = destinationDirectoryPath.substring(0, lastIndex);
    if (destinationDirectoryPath.charAt(0) == '/')
      destinationDirectoryPath = destinationDirectoryPath.substring(1);

    if (!(remoteFileNames.contains(destinationDirectoryPath))) {
      this.createFolder(s3client, bucketName, destinationDirectoryPath);
    }

    // compare files in the bucket with the local ones and populate filesToUpload list
    for (String fileName : localFileName2File.keySet()) {
      if (remoteFileNames.contains(destinationDirectoryPath + suffix + fileName)) {
        localFileName2File.remove(fileName);
      }
    }

    // upload each file contained in the filesToUpload list
    for (File currentFile : localFileName2File.values()) {
      s3client.putObject(
          new PutObjectRequest(
              bucketName, destinationDirectoryPath + suffix + currentFile.getName(), currentFile));
    }

    return true;
  }

  @Override
  public OUploadMetadata executeUpload(
      String sourceFile, String fName, String destinationDirectoryPath) {
    AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
    AmazonS3Client s3client = new AmazonS3Client(awsCredentials);
    return executeS3FileUpload(s3client, bucketName, sourceFile, destinationDirectoryPath);
  }

  protected OUploadMetadata executeS3FileUpload(
      AmazonS3 s3Client, String bucketName, String sourceFile, String destinationDirectoryPath) {
    long start = System.currentTimeMillis();

    // Do the Upload
    Map<String, String> metadata = new HashMap<>();
    metadata.putIfAbsent("directory", destinationDirectoryPath);
    metadata.putIfAbsent("bucketName", bucketName);

    if (!s3Client.doesBucketExist(bucketName)) {
      s3Client.createBucket(bucketName);
    }

    if (!s3Client.doesObjectExist(bucketName, destinationDirectoryPath)) {
      this.createFolder(s3Client, bucketName, destinationDirectoryPath);
    }

    File file = new File(sourceFile);
    s3Client.putObject(bucketName, destinationDirectoryPath + suffix + file.getName(), file);

    long end = System.currentTimeMillis();
    long elapsed = end - start;

    return new OUploadMetadata("s3", elapsed, metadata);
  }

  @Override
  public void config(ODocument cfg) {

    this.bucketName = cfg.field("bucketName");
    this.accessKey = System.getenv("BACKUP_AWS_ACCESS_KEY");
    this.secretKey = System.getenv("BACKUP_AWS_SECRET_KEY");

    if (this.bucketName == null || this.accessKey == null || this.secretKey == null) {
      throw new IllegalArgumentException(
          "Cannot configure the backup in s3. Parameters are missing");
    }
  }

  public void createFolder(AmazonS3 s3Client, String bucketName, String folderName) {

    // create meta-data for your folder and set content-length to 0
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(0);

    // create empty content
    InputStream emptyContent = new ByteArrayInputStream(new byte[0]);

    // create a PutObjectRequest passing the folder name suffixed by /
    PutObjectRequest putObjectRequest =
        new PutObjectRequest(bucketName, folderName + suffix, emptyContent, metadata);

    // send request to S3 to create folder
    s3Client.putObject(putObjectRequest);
  }

  @Override
  public String executeDownload(OBackupUploadFinishedLog upload) {

    AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
    AmazonS3Client s3client = new AmazonS3Client(awsCredentials);

    Map<String, String> metadata = upload.getMetadata();

    String directory = metadata.get("directory");
    String bucketName = metadata.get("bucketName");

    return executeS3Download(s3client, bucketName, directory);
  }

  protected String executeS3Download(AmazonS3 s3Client, String bucketName, String directory) {
    ObjectListing objectListing = s3Client.listObjects(bucketName, directory);

    try {
      Path tempDir = Files.createTempDirectory(directory);
      for (S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {

        String[] strings = s3ObjectSummary.getKey().split("/");

        if (strings.length > 1) {
          S3Object object = s3Client.getObject(bucketName, s3ObjectSummary.getKey());
          Files.copy(object.getObjectContent(), tempDir.resolve(strings[1]));
        }
      }
      return tempDir.toString();
    } catch (IOException e) {
      logger.error("Error %s", e, e.getMessage());
    }

    return null;
  }
}

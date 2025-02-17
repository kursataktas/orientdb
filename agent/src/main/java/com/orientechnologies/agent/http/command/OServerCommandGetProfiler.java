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
package com.orientechnologies.agent.http.command;

import com.orientechnologies.agent.EnterprisePermissions;
import com.orientechnologies.agent.profiler.OEnterpriseProfiler;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.serialization.serializer.OJSONWriter;
import com.orientechnologies.orient.server.network.protocol.http.OHttpRequest;
import com.orientechnologies.orient.server.network.protocol.http.OHttpResponse;
import com.orientechnologies.orient.server.network.protocol.http.OHttpUtils;
import com.orientechnologies.orient.server.network.protocol.http.command.OServerCommandAuthenticatedServerAbstract;
import java.io.StringWriter;

public class OServerCommandGetProfiler extends OServerCommandAuthenticatedServerAbstract {
  private static final String[] NAMES = {"GET|profiler/*", "POST|profiler/*"};

  public OServerCommandGetProfiler() {
    super(EnterprisePermissions.SERVER_PROFILER.toString());
  }

  @Override
  public boolean execute(final OHttpRequest iRequest, OHttpResponse iResponse) throws Exception {
    final String[] parts =
        checkSyntax(iRequest.getUrl(), 2, "Syntax error: profiler/<command>/[<config>]|[<from>]");

    iRequest.getData().commandInfo = "Profiler information";

    try {

      final String command = parts[1];
      final String arg = parts.length > 2 ? parts[2] : null;

      if (command.equalsIgnoreCase("start")) {
        Orient.instance().getProfiler().startRecording();
        iResponse.send(
            OHttpUtils.STATUS_OK_CODE,
            "OK",
            OHttpUtils.CONTENT_TEXT_PLAIN,
            "Recording started",
            null);

      } else if (command.equalsIgnoreCase("stop")) {
        Orient.instance().getProfiler().stopRecording();
        iResponse.send(
            OHttpUtils.STATUS_OK_CODE,
            "OK",
            OHttpUtils.CONTENT_TEXT_PLAIN,
            "Recording stopped",
            null);

      } else if (command.equalsIgnoreCase("configure")) {
        Orient.instance().getProfiler().configure(parts[2]);
        iResponse.send(
            OHttpUtils.STATUS_OK_CODE,
            "OK",
            OHttpUtils.CONTENT_TEXT_PLAIN,
            "Profiler configured with: " + parts[2],
            null);

      } else if (command.equalsIgnoreCase("status")) {
        final String status = Orient.instance().getProfiler().isRecording() ? "on" : "off";
        iResponse.send(
            OHttpUtils.STATUS_OK_CODE, "OK", OHttpUtils.CONTENT_TEXT_PLAIN, status, null);

      } else if (command.equalsIgnoreCase("reset")) {
        Orient.instance().getProfiler().resetRealtime(arg);
        iResponse.send(
            OHttpUtils.STATUS_OK_CODE, "OK", OHttpUtils.CONTENT_TEXT_PLAIN, "deleted", null);

      } else if (command.equalsIgnoreCase("restart")) {
        Orient.instance().getProfiler().stopRecording();
        Orient.instance().getProfiler().startRecording();
        iResponse.send(
            OHttpUtils.STATUS_OK_CODE, "OK", OHttpUtils.CONTENT_JSON, "profiler restarted", null);

      } else if (command.equalsIgnoreCase("metadata")) {
        iResponse.send(
            OHttpUtils.STATUS_OK_CODE,
            "OK",
            OHttpUtils.CONTENT_JSON,
            Orient.instance().getProfiler().metadataToJSON(),
            null);
      } else {
        final StringWriter jsonBuffer = new StringWriter();
        final OJSONWriter json = new OJSONWriter(jsonBuffer);
        OEnterpriseProfiler profiler = (OEnterpriseProfiler) Orient.instance().getProfiler();
        profiler.updateStats();
        json.append(Orient.instance().getProfiler().toJSON(command, arg));
        iResponse.send(
            OHttpUtils.STATUS_OK_CODE, "OK", OHttpUtils.CONTENT_JSON, jsonBuffer.toString(), null);
      }

    } catch (Exception e) {
      iResponse.send(
          OHttpUtils.STATUS_BADREQ_CODE,
          OHttpUtils.STATUS_BADREQ_DESCRIPTION,
          OHttpUtils.CONTENT_TEXT_PLAIN,
          e,
          null);
    }
    return false;
  }

  @Override
  public String[] getNames() {
    return NAMES;
  }
}

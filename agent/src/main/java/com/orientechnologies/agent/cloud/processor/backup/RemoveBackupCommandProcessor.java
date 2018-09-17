package com.orientechnologies.agent.cloud.processor.backup;

import com.orientechnologies.agent.OEnterpriseAgent;
import com.orientechnologies.agent.cloud.CloudException;
import com.orientechnologies.agent.cloud.processor.tasks.backup.RemoveBackupTask;
import com.orientechnologies.agent.operation.NodeResponse;
import com.orientechnologies.agent.operation.OperationResponseFromNode;
import com.orientechnologies.agent.operation.ResponseOk;
import com.orientechnologies.agent.services.backup.OBackupService;
import com.orientechnologies.orientdb.cloud.protocol.Command;
import com.orientechnologies.orientdb.cloud.protocol.CommandResponse;
import com.orientechnologies.orientdb.cloud.protocol.backup.BackupLogRequest;

public class RemoveBackupCommandProcessor extends AbstractBackupCommandProcessor {
  @Override
  public CommandResponse execute(Command command, OEnterpriseAgent agent) {

    CommandResponse response = fromRequest(command);

    BackupLogRequest request = (BackupLogRequest) command.getPayload();

    if (!agent.isDistributed()) {
      removeBackup(agent, request);
    } else {

      OperationResponseFromNode res = agent.getNodesManager().send(request.getServer(), new RemoveBackupTask(request));
      NodeResponse nodeResponse = res.getNodeResponse();

      if (nodeResponse.getResponseType() == 1) {
        ResponseOk ok = (ResponseOk) nodeResponse;
      } else {
        throw new CloudException("", 500, String.format("Cannot execute request on node %d", request.getServer()), "");
      }

    }
    response.setPayload("");
    return response;
  }

  public static void removeBackup(OEnterpriseAgent agent, BackupLogRequest request) {
    OBackupService backupService = agent.getServiceByClass(OBackupService.class).get();

    if (request.getUnitId() != null && request.getTxId() != null) {
      backupService.deleteBackup(request.getBackupId(), request.getUnitId(), request.getTxId());
    } else {
      backupService.removeAndStopBackup(request.getBackupId());
    }
  }

}

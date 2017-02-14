/*
 * Copyright (c) 2014 Contextream, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.ha;

import java.util.List;

import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.sfc.provider.api.SfcProviderServiceTypeAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.ServiceFunctionType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sft.rev140701.service.function.types.service.function.type.SftServiceFunctionName;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.sf.desc.mon.rev141201.service.functions.state.service.function.state.SfcSfDescMon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * Created by BOO on 2017-01-31.
 */
public class SfcHAmechanism {

    private static final Logger LOG = LoggerFactory.getLogger(SfcHAmechanism.class);

    private static final String GOOD = "good";
    private static final String SELECTION = "selection";
    private static final String SELECTIONMIGRATION= "selection-migration";
    private static final String MIGRATION= "migration";
    private static final String DELETESF= "delete-backupSF";

    public static void SfcFailoverPrediction (SfName sfName) {

        GetPredictionDynamicThread PredictionSFDynamicThread = new GetPredictionDynamicThread(sfName.getValue());
        Thread thread = new Thread(PredictionSFDynamicThread);
        thread.start();

    }

    public static void SfcFailover (SfName sfName) {

         ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfName);
         SfName backupsfName = null;
         backupsfName = serviceFunction.getBackupSf();

         if (backupsfName != null) {
           ServiceFunction backupserviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(backupsfName);
           SfcHAMigrationAPI.HAmigration (serviceFunction, backupserviceFunction, true);
         } else {
           backupsfName = selectBackupServiceFunction(serviceFunction);
           ServiceFunction backupserviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(backupsfName);
           SfcHAMigrationAPI.HAmigration (serviceFunction, backupserviceFunction, true);
         }
    }

    // policy driven
    // input : ServiceFunction serviceFunction, policy 1, policy 2, boolean failover
    // output is one of good, selection, selection-migration, migration;
    public static String predictionSf(ServiceFunction serviceFunction, int policy1, int policy2) {
        // bsfName was allocated backup SF name when SF is created
        // bsfName_s is selection backup SF when allocated backup SF isn't
        String output = null;
        SfName sfName = serviceFunction.getName();
        SfName bsfName = serviceFunction.getBackupSf();
        SfName bsfName_s = SfcHAServiceFunctionAPI.readBackupServiceFunction(sfName);

        /* Read ServiceFunctionMonitor information */
        SfcSfDescMon sfcSfDescMon = SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitor(sfName);
        if (sfcSfDescMon == null) {
            LOG.error("Read monitor information from Data Store failed! serviceFunction: {}", sfName);
        }
        java.lang.Long CPUutil = sfcSfDescMon.getMonitoringInfo().getResourceUtilization().getCPUUtilization();

        if (CPUutil.intValue() > policy2) {
            if (bsfName == null) {
                if (bsfName_s != null && bsfName_s.getValue() != "old") {
                    output = MIGRATION;
                } else {
                    output = SELECTIONMIGRATION;
                }
            } else {
                output = MIGRATION;
            }
        } else if (CPUutil.intValue() > policy1 ) {
            if (bsfName == null) {
                if (bsfName_s != null && bsfName_s.getValue() != "old") {
                    output = GOOD;
                } else {
                    output = SELECTION;
                }
            } else {
                output = GOOD;
            }
        } else  {
            if (bsfName == null) {
                if (bsfName_s == null || bsfName_s.getValue() == "old") {
                    output = GOOD;
                } else {
                   output = DELETESF;
                }
            } else {
                output = GOOD;
            }
        }
            LOG.info("result {} ==> {} utilization : {}, backupSF : {}, backupSF : {}, policy1 :{}, policy2 :{}",output, sfName, CPUutil.intValue(), bsfName, bsfName_s, policy1, policy2);

        return output;
    }


    // selection function for backup-sf
    public static SfName selectBackupServiceFunction(ServiceFunction serviceFunction) {

            SfName sftServiceFunctionName = null;
            SfName sfName = serviceFunction.getName();
            ServiceFunctionType serviceFunctionType = SfcProviderServiceTypeAPI.readServiceFunctionType(serviceFunction.getType());
            List<SftServiceFunctionName> sftServiceFunctionNameList = serviceFunctionType.getSftServiceFunctionName();
            LOG.info ("candidate sfs are{}", sftServiceFunctionNameList.size());
            SfName sfName_backup = null;

            java.lang.Long preCPUUtilization = java.lang.Long.MAX_VALUE;


            // TODO As part of typedef refactor not message with SFTs
            for (SftServiceFunctionName curSftServiceFunctionName : sftServiceFunctionNameList) {
                sfName_backup = new SfName(curSftServiceFunctionName.getName());
                    LOG.info("Candidate SF {}", sfName_backup);
            /* Check next one if curSftServiceFunctionName doesn't exist */
                ServiceFunction serviceFunction_backupsf = SfcProviderServiceFunctionAPI.readServiceFunction(new SfName (sfName_backup.getValue()));
                if (serviceFunction_backupsf == null) {
                    LOG.info("ServiceFunction {} doesn't exist or original ServiceFunction", sfName_backup);
                    continue;
                }

            /* Read ServiceFunctionMonitor information */
                SfcSfDescMon sfcSfDescMon = SfcProviderServiceFunctionAPI.readServiceFunctionDescriptionMonitor(new SfName (sfName_backup.getValue()));
                if (sfcSfDescMon == null) {
                    // TODO As part of typedef refactor not message with SFTs
                    sftServiceFunctionName = sfName_backup;
                    LOG.error("Read monitor information from Data Store failed! serviceFunction: {}", sfName_backup);
                    // Use sfName if no sfcSfDescMon is availble
                    break;
                }

                java.lang.Long curCPUUtilization =
                        sfcSfDescMon.getMonitoringInfo().getResourceUtilization().getCPUUtilization();

                if (preCPUUtilization > curCPUUtilization) {
                    preCPUUtilization = curCPUUtilization;
                    sftServiceFunctionName = sfName_backup;
                }
            }

           if (sftServiceFunctionName == null) {
                LOG.info("There is not available Backup Service Function for {}", serviceFunctionType.getType());

            }

              LOG.info("The selected backup sf name : {}", sftServiceFunctionName.getValue());
              return sftServiceFunctionName;


    }
}

class GetPredictionDynamicThread implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(GetPredictionDynamicThread.class);
    private int ticket = 10;
    private String nodeName;
    int policy1 = 80;
    int policy2 = 90;

    private static final String GOOD = "good";
    private static final String SELECTION = "selection";
    private static final String SELECTIONMIGRATION= "selection-migration";
    private static final String MIGRATION= "migration";
    private static final String DELETESF= "delete-bakcupSF";

    public GetPredictionDynamicThread (String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public void run() {
        while (true) {
            printTraceStart(LOG);
            SfName sfNodeName = new SfName(nodeName);
            ServiceFunction serviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(sfNodeName);

            String prediction_state = SfcHAmechanism.predictionSf(serviceFunction, policy1, policy2);
            switch (prediction_state) {
                case GOOD : {
                    break;
                }

                case SELECTION : {
                    SfName backupSfName = SfcHAmechanism.selectBackupServiceFunction(serviceFunction);
                    SfcHAServiceFunctionAPI.mergeBackupSfSelection(backupSfName, sfNodeName);
                    break;
                }

                case SELECTIONMIGRATION : {
                    SfName backupSfName = SfcHAmechanism.selectBackupServiceFunction(serviceFunction);
                    SfcHAServiceFunctionAPI.mergeBackupSfSelection(backupSfName, sfNodeName);
                    ServiceFunction backupserviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(backupSfName);
                    SfcHAMigrationAPI.HAmigration ( serviceFunction, backupserviceFunction, false);
                    break;
                }

                case MIGRATION : {
                    SfName backupSfName = SfcHAServiceFunctionAPI.readBackupServiceFunction(serviceFunction.getName());
                    ServiceFunction backupserviceFunction = SfcProviderServiceFunctionAPI.readServiceFunction(backupSfName);
                    SfcHAMigrationAPI.HAmigration (serviceFunction, backupserviceFunction, false);
                    break;
                }
                case DELETESF : {
                    SfName backupSfName = new SfName ("old");
                    SfcHAServiceFunctionAPI.mergeBackupSfSelection(backupSfName, sfNodeName);
                }
                default: {
                    break;
                }

            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LOG.warn("failed to ....", e);
            }
            printTraceStop(LOG);
        }
    }
}


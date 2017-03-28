/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.OFM;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

import org.opendaylight.sfc.provider.api.*;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctionsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.ServiceFunctionStateKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by BOO on 2017-02-02.
 */


public class SfcOFMServiceFunctionAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOFMServiceFunctionAPI.class);

    public static boolean mergeBackupSfSelection(SfName backupsfName, SfName sfName) {

        boolean ret = false;
        printTraceStart(LOG);
        ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(sfName);
        InstanceIdentifier<ServiceFunctionState> sfStateIID =
                InstanceIdentifier.builder(ServiceFunctionsState.class)
                        .child(ServiceFunctionState.class, serviceFunctionStateKey)
                        .build();

        ServiceFunctionState serviceFunctionState = new ServiceFunctionStateBuilder()
                .setKey(serviceFunctionStateKey).setBackupSfSelection(backupsfName).build();

        ret = SfcDataStoreAPI.writeMergeTransactionAPI(sfStateIID, serviceFunctionState, LogicalDatastoreType.OPERATIONAL);
        printTraceStop(LOG);
        return ret;
    }

    public static SfName readBackupServiceFunction (SfName serviceFunctionName) {
        printTraceStart(LOG);

        SfName ret = null;
        ServiceFunctionStateKey serviceFunctionStateKey = new ServiceFunctionStateKey(serviceFunctionName);
        InstanceIdentifier<ServiceFunctionState> sfStateIID = InstanceIdentifier.builder(ServiceFunctionsState.class)
                .child(ServiceFunctionState.class, serviceFunctionStateKey)
                .build();

        ServiceFunctionState dataSfcStateObject;
        dataSfcStateObject = SfcDataStoreAPI.readTransactionAPI(sfStateIID, LogicalDatastoreType.OPERATIONAL);
        // Read the list of Service Function Path anchored by this SFF
        if (dataSfcStateObject != null) {
            ret = dataSfcStateObject.getBackupSfSelection();
        } else {
            LOG.warn("readServiceFunctionDescriptionMonitor() Service Function {} has no operational state",
                    serviceFunctionName);
        }
        printTraceStop(LOG);
        return ret;
    }

}

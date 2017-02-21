/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.la;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfcName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.ServiceFunctionChainStateKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainsState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.ServiceFunctionChainsStateBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.service.function.chain.state.SfcServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.service.function.chain.state.SfcServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chains.state.service.function.chain.state.SfcServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * This class has the APIs to operate on the ServiceFunctionChain
 * datastore.
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Jaewook Lee (iioiioiio12345@gmail.com)
 */
public class SfcHAServiceChainAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcHAServiceChainAPI.class);
    //delete sfp
    public static boolean  deleteSFPtoServiceFunctionChainState(SfcName sfcName) {
        boolean ret = false;

        ServiceFunctionChainStateKey serviceFunctionChainStateKey = (new ServiceFunctionChainStateKey(sfcName));

        InstanceIdentifier<ServiceFunctionChainsState> sfcsIID = InstanceIdentifier.builder(ServiceFunctionChainsState.class).build();

        ret = SfcDataStoreAPI.deleteTransactionAPI(sfcsIID,LogicalDatastoreType.OPERATIONAL);
        return ret;
    }

    //insert sfp
    public static boolean  putSFPtoServiceFunctionChainState(List <ServiceFunctionPath> sfpList, SfcName sfcName) {
        boolean ret = false;

        ServiceFunctionChainStateBuilder serviceFunctionChainStateBuilder = new ServiceFunctionChainStateBuilder();
        ServiceFunctionChainsStateBuilder serviceFunctionChainsStateBuilder = new ServiceFunctionChainsStateBuilder();
        List<ServiceFunctionChainState> serviceFunctionChainStateList = new ArrayList<>();
        List<SfcServicePath> sfcServicePathList = new ArrayList<>();

        for (ServiceFunctionPath sfp : sfpList) {
            SfcServicePathBuilder sfcServicePathBuilder = new SfcServicePathBuilder();
            sfcServicePathBuilder.setName(sfp.getName()).setKey(new SfcServicePathKey(sfp.getName()));
            sfcServicePathList.add(sfcServicePathBuilder.build());
        }

        serviceFunctionChainStateBuilder.setName(sfcName)
                .setKey(new ServiceFunctionChainStateKey(sfcName))
                .setSfcServicePath(sfcServicePathList);
        serviceFunctionChainStateList.add(serviceFunctionChainStateBuilder.build());

        serviceFunctionChainsStateBuilder.setServiceFunctionChainState(serviceFunctionChainStateList);
        // create instance identifier
        InstanceIdentifier<ServiceFunctionChainsState> sfcsIID =
                InstanceIdentifier.builder(ServiceFunctionChainsState.class).build();

        // write transaction
        ret = SfcDataStoreAPI.writePutTransactionAPI(sfcsIID,
                serviceFunctionChainsStateBuilder.build(), LogicalDatastoreType.OPERATIONAL);
        return ret;
    }

}

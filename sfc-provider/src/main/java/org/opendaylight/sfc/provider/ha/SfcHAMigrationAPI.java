/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.ha;

//import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathStateBuilder;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathStateKey;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
/**
 * Created by BOO on 2017-02-08.
 */
public class SfcHAMigrationAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcHAMigrationAPI.class);

    public static void HAmigration(ServiceFunction serviceFunction, ServiceFunction backupServiceFunction, boolean failover) {

        List <ServiceFunctionPath>  backupservicePathList = new ArrayList<>();
        List <ServiceFunctionPath> serviceFunctionPathList = new ArrayList<>();
        List <SfServicePath> sfServicePathList = new ArrayList<>();
        List <RenderedServicePath> renderedServicePathList = new ArrayList<>();
        List <SfName> sfNameList = new ArrayList<>();
        SfName backupSfName = backupServiceFunction.getName();
        boolean ret = false;
        int N = 1;
        SfName oldSfName = serviceFunction.getName();
        SfName newSfName = backupServiceFunction.getName();

        // read sfp allocate to SF and sfp
        if (failover == true) {
          sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionState(serviceFunction.getName());
        } else if (failover == false) {
         List <SfServicePath> sfServicePathList_all = SfcProviderServiceFunctionAPI.readServiceFunctionState(serviceFunction.getName());
         for (int ii = 0 ; ii < N ; ii++) {
             sfServicePathList.add (sfServicePathList_all.get(ii));
          }
        }

        ServiceFunctionPathStateBuilder serviceFunctionPathStateBuilder = new ServiceFunctionPathStateBuilder();

        for (SfServicePath sFPath : sfServicePathList) {
            ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();

            SfpName sfpName = new SfpName(sFPath.getName().getValue());
            ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(sfpName);
            List<ServicePathHop> servicePathHopList = serviceFunctionPath.getServicePathHop();
            List<ServicePathHop> backupservicePathHopList = new ArrayList<>();

            for (ServicePathHop sphop : servicePathHopList){
                 SfName sfName = null;
                 ServicePathHopBuilder servicePathHopBuilder = new ServicePathHopBuilder();
                //check the oldSF
                if (sphop.getServiceFunctionName().getValue() != oldSfName.getValue()) {
                    sfName = sphop.getServiceFunctionName();
                } else {
                    sfName = backupSfName;
                }
                servicePathHopBuilder.setHopNumber(sphop.getHopNumber())
                        .setKey(sphop.getKey())
//                        .setServiceFunctionForwarder(sphop.getServiceFunctionForwarder())
                        .setServiceFunctionGroupName(sphop.getServiceFunctionGroupName())
                        .setServiceIndex(sphop.getServiceIndex())
                        .setServiceFunctionName(sfName);
                backupservicePathHopList.add(servicePathHopBuilder.build());
            }

            // copy servingpath info to backupservicepath
            serviceFunctionPathBuilder.setName(serviceFunctionPath.getName())
                    .setKey(new ServiceFunctionPathKey(serviceFunctionPath.getName()))
//                    .setSfcEncapsulation(serviceFunctionPath.getSfcEncapsulation())
                    .setClassifier(serviceFunctionPath.getClassifier())
                    .setContextMetadata(serviceFunctionPath.getContextMetadata())
                    .setVariableMetadata(serviceFunctionPath.getVariableMetadata())
                    .setTenantId(serviceFunctionPath.getTenantId())
                    .setServicePathHop(servicePathHopList)
                    .setServiceChainName(serviceFunctionPath.getServiceChainName())
                    .setStartingIndex(serviceFunctionPath.getStartingIndex())
                    .setPathId(serviceFunctionPath.getPathId());
            backupservicePathList.add(serviceFunctionPathBuilder.build());

            ret = SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionState(serviceFunctionPath.getName());
            ret = SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(serviceFunctionPath);
            SfcName sfcName = serviceFunctionPath.getServiceChainName();
            SfcHAServiceChainAPI.deleteSFPtoServiceFunctionChainState(sfcName);

        }

        for (ServiceFunctionPath serviceFunctionPath : backupservicePathList) {
            List<ServicePathHop> servicePathHopList1 = serviceFunctionPath.getServicePathHop();
            SfcName sfcName = serviceFunctionPath.getServiceChainName();
            List <ServiceFunctionPath> sfpList = new ArrayList<>();
            sfpList.add(serviceFunctionPath);
            SfcHAServiceChainAPI.putSFPtoServiceFunctionChainState(sfpList, sfcName);
            sfNameList = new ArrayList<>();
            ret = SfcProviderServicePathAPI.putServiceFunctionPath(serviceFunctionPath);
            for (int i = 0; i < servicePathHopList1.size(); i++){
                SfName sfName1 = servicePathHopList1.get(i).getServiceFunctionName();
                if (sfName1 == null) {
                    continue;
                }
                else {
                    sfNameList.add(sfName1);
                }
            }

            List <SfpRenderedServicePath> sfpRspList = SfcProviderServicePathAPI.readServicePathState(serviceFunctionPath.getName());
            for (SfpRenderedServicePath sfpRsp : sfpRspList) {
               RspName rspName = new RspName (sfpRsp.getName().getValue());
               ret = SfcProviderRenderedPathAPI.deleteRenderedServicePath(rspName);

               CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
               createRenderedPathInputBuilder.setName(rspName.getValue()).setParentServiceFunctionPath(serviceFunctionPath.getName().getValue());
               CreateRenderedPathInput createRenderedPathInput = createRenderedPathInputBuilder.build();
               renderedServicePathList.add(SfcHARenderedPathAPI.createFailoverRenderedServicePathAndState (serviceFunctionPath, createRenderedPathInput , sfNameList));
             }
            }




    }

}

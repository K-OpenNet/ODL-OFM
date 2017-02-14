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
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathBuilder;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathState;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathStateBuilder;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.ServiceFunctionPathStateKey;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPathKey;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHop;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.service.function.path.ServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.state.service.function.state.SfServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInputBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.RspName;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.state.service.function.path.state.SfpRenderedServicePath;

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

         List <SfServicePath> sfServicePathList = new ArrayList<>();
         List <RspName> RspList = new ArrayList<>();
         SfName backupSfName = backupServiceFunction.getName();
         SfName oldSfName = serviceFunction.getName();
         List <RenderedServicePath> renderedServicePathList = new ArrayList<>();
         boolean ret = false;
         int N = 1;
          List <SfName> sfNameList = new ArrayList<>();

        // read RSP allocate to SF
        // sfServicePath = RSP
        if (failover == true) {
          sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionState(oldSfName);
          LOG.info(" failover detection, sf : {}, sfp : {}", serviceFunction.getName().getValue(), sfServicePathList.size());
        } else if (failover == false) {
         List <SfServicePath> sfServicePathList_all = SfcProviderServiceFunctionAPI.readServiceFunctionState(oldSfName);
          LOG.info(" overload detection, sf : {}, sfp : {}", serviceFunction.getName().getValue(), sfServicePathList_all.size());
         for (int ii = 0 ; ii < N ; ii++) {
             sfServicePathList.add (sfServicePathList_all.get(ii));
          }
          LOG.info(" overload detection, sf : {}, sfp : {}", serviceFunction.getName().getValue(), sfServicePathList.size());
        }

        for (SfServicePath sFPath : sfServicePathList) {

            RspName rspName = new RspName (sFPath.getName().getValue());
            LOG.info(" The RSP {} is allocated to SFP {}", rspName, sFPath.getName());

            RenderedServicePath renderedServicePath =SfcProviderRenderedPathAPI.readRenderedServicePath(new RspName (rspName.getValue()));
            List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
            sfNameList = new ArrayList<>();

            for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
                SfName SfcSfName = new SfName (renderedServicePathHop.getServiceFunctionName().getValue());
                if (SfcSfName.getValue() ==  oldSfName.getValue()) {
                   SfcSfName = new SfName(backupSfName.getValue());
                }
                sfNameList.add(SfcSfName);
                LOG.info ("SF for chain is {} ", SfcSfName);
               }

               ret = SfcProviderRenderedPathAPI.deleteRenderedServicePath(rspName);
               CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
               createRenderedPathInputBuilder.setName(rspName.getValue()).setParentServiceFunctionPath(renderedServicePath.getParentServiceFunctionPath().getValue());
               CreateRenderedPathInput createRenderedPathInput = createRenderedPathInputBuilder.build();
               ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(new SfpName (renderedServicePath.getParentServiceFunctionPath().getValue()));
               renderedServicePathList.add(SfcHARenderedPathAPI.createFailoverRenderedServicePathAndState (serviceFunctionPath, createRenderedPathInput , sfNameList));
        }
   }
}



/*
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
          sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionState(oldSfName);
          LOG.info(" failover detection, sf : {}, sfp : {}", serviceFunction.getName().getValue(), sfServicePathList.size());
        } else if (failover == false) {
         List <SfServicePath> sfServicePathList_all = SfcProviderServiceFunctionAPI.readServiceFunctionState(oldSfName);
          LOG.info(" overload detection, sf : {}, sfp : {}", serviceFunction.getName().getValue(), sfServicePathList_all.size());
         for (int ii = 0 ; ii < N ; ii++) {
             sfServicePathList.add (sfServicePathList_all.get(ii));
          }
          LOG.info(" overload detection, sf : {}, sfp : {}", serviceFunction.getName().getValue(), sfServicePathList.size());
        }





 //       ServiceFunctionPathStateBuilder serviceFunctionPathStateBuilder = new ServiceFunctionPathStateBuilder();

        for (SfServicePath sFPath : sfServicePathList) {
//            ServiceFunctionPathBuilder serviceFunctionPathBuilder = new ServiceFunctionPathBuilder();
            ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(new SfpName(sFPath.getName().getValue()));
            LOG.info(" overload detection, sfp {}", serviceFunctionPath);
//            List<ServicePathHop> servicePathHopList = serviceFunctionPath.getServicePathHop();
//            List<ServicePathHop> backupservicePathHopList = new ArrayList<>();

//            for (ServicePathHop sphop : servicePathHopList){
//                 SfName sfName = null;
//                 ServicePathHopBuilder servicePathHopBuilder = new ServicePathHopBuilder();
                //check the oldSF
//                if (sphop.getServiceFunctionName().getValue() != oldSfName.getValue()) {
//                    sfName = sphop.getServiceFunctionName();
//                } else {
//                    sfName = backupSfName;
//                }
//                servicePathHopBuilder.setHopNumber(sphop.getHopNumber())
//                        .setKey(sphop.getKey())
//                        .setServiceFunctionForwarder(sphop.getServiceFunctionForwarder())
//                        .setServiceFunctionGroupName(sphop.getServiceFunctionGroupName())
//                        .setServiceIndex(sphop.getServiceIndex())
//                        .setServiceFunctionName(new SfName (sfName.getValue()));
//                backupservicePathHopList.add(servicePathHopBuilder.build());
//            }

            // copy servingpath info to backupservicepath
//            serviceFunctionPathBuilder.setName(new SfpName(sFPath.getName().getValue()))
//                    .setKey(new ServiceFunctionPathKey(new SfpName(sFPath.getName().getValue())));
//                    .setSfcEncapsulation(serviceFunctionPath.getSfcEncapsulation())
//                    .setClassifier(serviceFunctionPath.getClassifier())
//                    .setContextMetadata(serviceFunctionPath.getContextMetadata())
//                    .setVariableMetadata(serviceFunctionPath.getVariableMetadata())
//                    .setTenantId(serviceFunctionPath.getTenantId())
//                    .setServicePathHop(servicePathHopList)
//                    .setServiceChainName(serviceFunctionPath.getServiceChainName())
//                    .setStartingIndex(serviceFunctionPath.getStartingIndex())
//                    .setPathId(serviceFunctionPath.getPathId());

//            backupservicePathList.add(serviceFunctionPathBuilder.build());
//        }

//        LOG.info ("copy backupsfplist size : {}", backupservicePathList.size());

//        for (ServiceFunctionPath serviceFunctionPath : backupservicePathList) {

            List <SfpRenderedServicePath> sfpRspList = SfcProviderServicePathAPI.readServicePathState(new SfpName(sFPath.getName().getValue()));
            LOG.info ("{} RSPs are allocated to ", sfpRspList.size());

            ret = SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionState(new SfpName(sFPath.getName().getValue()));
            LOG.info ("delete sfp from sf : {} ", ret);
//          ret = SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(serviceFunctionPath);
//            LOG.info ("delete sfp from sff : {} ", ret);

//            List <ServiceFunctionPath> sfpList = new ArrayList<>();
//            sfpList.add(serviceFunctionPath);
//              SfcName sfcName = serviceFunctionPath.getServiceChainName();
//            SfcHAServiceChainAPI.deleteSFPtoServiceFunctionChainState(new SfcName (sfcName.getValue()));
//            LOG.info ("delete sfp from sfc : {} ", ret);
//            SfcHAServiceChainAPI.putSFPtoServiceFunctionChainState(sfpList, new SfcName (sfcName.getValue()));
//            LOG.info ("put sfp to sfc : {} ", ret);
//            ret = SfcProviderServicePathAPI.deleteServiceFunctionPath(serviceFunctionPath);
//            LOG.info ("delete sfp from database : {} ", ret);
//            ret = SfcProviderServicePathAPI.putServiceFunctionPath(serviceFunctionPath);
//            LOG.info ("put sfp to database : {} ", ret);

            for (SfpRenderedServicePath sfpRsp : sfpRspList) {
               RspName rspName = new RspName (sfpRsp.getName().getValue());
               RenderedServicePath renderedServicePath =SfcProviderRenderedPathAPI.readRenderedServicePath(new RspName (rspName.getValue()));
               List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
               sfNameList = new ArrayList<>();
               for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
                SfName SfcSfName = new SfName (renderedServicePathHop.getServiceFunctionName().getValue());
                if (SfcSfName.getValue() ==  backupSfName.getValue()) {
                   SfcSfName = new SfName(backupSfName.getValue());
                }
                sfNameList.add(SfcSfName);
               LOG.info ("SF for chain is {} ", SfcSfName);
               }

               ret = SfcProviderRenderedPathAPI.deleteRenderedServicePath(new RspName (rspName.getValue()));
               CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
               createRenderedPathInputBuilder.setName(rspName.getValue()).setParentServiceFunctionPath(serviceFunctionPath.getName().getValue());
               CreateRenderedPathInput createRenderedPathInput = createRenderedPathInputBuilder.build();
               renderedServicePathList.add(SfcHARenderedPathAPI.createFailoverRenderedServicePathAndState (serviceFunctionPath, createRenderedPathInput , sfNameList));
           }
       }
   }

}
*/

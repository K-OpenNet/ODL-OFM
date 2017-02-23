/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.la;

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
public class SfcLAMigrationAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcLAMigrationAPI.class);

    //TODO we should defince migration modules witch one is for failure and other is for overload
    public static void failurePathMigration(ServiceFunction serviceFunction, List <SfName> backupSfNameList) {

        List <RspName> RspList = new ArrayList<>();
        List <RenderedServicePath> renderedServicePathList = new ArrayList<>();
        List <SfName> sfNameList = new ArrayList<>();
        int n_bakupSf = backupSfNameList.size();
        boolean ret = false;

        // Read all RSPs which are allocated into failure service function
        SfName oldSfName = serviceFunction.getName();
        List <SfServicePath> sfServicePathList = new ArrayList<>();
        sfServicePathList = SfcProviderServiceFunctionAPI.readServiceFunctionState(oldSfName);
        int n_Sfp = sfServicePathList.size();

        if (n_Sfp == 0) {

            LOG.info(" There is no SFP allocated into failure SF : {},",oldSfName.getValue());

        } else {
            LOG.info(" SF  {} and {} SFPs are failure.",oldSfName.getValue(), n_Sfp);

            for (SfServicePath sFPath : sfServicePathList) {
                //TODO : Modify to loadbalancing

                SfName backupSfName = backupSfNameList.get(0);
                RspName rspName = new RspName (sFPath.getName().getValue());
                LOG.info("The RSP {} which is allocated to SFP {} should be migrated to new RSP", rspName, sFPath.getName());

                RenderedServicePath renderedServicePath =SfcProviderRenderedPathAPI.readRenderedServicePath(new RspName (rspName.getValue()));
                List<RenderedServicePathHop> renderedServicePathHopList = renderedServicePath.getRenderedServicePathHop();
                // Create new RSP instance
                sfNameList = new ArrayList<>();
                for (RenderedServicePathHop renderedServicePathHop : renderedServicePathHopList) {
                    SfName SfcSfName = new SfName (renderedServicePathHop.getServiceFunctionName().getValue());
                    if (SfcSfName.getValue() ==  oldSfName.getValue()) {
                        SfcSfName = new SfName(backupSfName.getValue());
                    }
                    sfNameList.add(SfcSfName);
                    LOG.info ("SF for chain is {} ", SfcSfName);
                }


                ret = SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionState(new SfpName (sFPath.getName().getValue()));
                if (ret == false) {
                    LOG.info("Old RSP is not deleted at SF DB");
                } else {
                    LOG.info("Old RSP is deleted at SF DB");
                }
                ret = SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(rspName);
                if (ret == false) {
                    LOG.info("Old RSP is not deleted at SFF DB");
                } else {
                    LOG.info("Old RSP is deleted at SFF DB");
                }
                ret = SfcProviderRenderedPathAPI.deleteRenderedServicePath(rspName);
                if (ret == false) {
                    LOG.info("Old RSP is not deleted at RSP DB");
                } else {
                    LOG.info("Old RSP is deleted at RSP DB");
                }

                CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
                createRenderedPathInputBuilder.setName(rspName.getValue()).setParentServiceFunctionPath(renderedServicePath.getParentServiceFunctionPath().getValue());
                CreateRenderedPathInput createRenderedPathInput = createRenderedPathInputBuilder.build();
                ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(new SfpName (renderedServicePath.getParentServiceFunctionPath().getValue()));
                renderedServicePathList.add(SfcLARenderedPathAPI.createFailoverRenderedServicePathAndState (serviceFunctionPath, createRenderedPathInput , sfNameList));
            }

        }
    }

    public static void overloadPathMigration(ServiceFunction serviceFunction, List <SfName> backupSfNameList) {

         SfName oldSfName = serviceFunction.getName();
         List <SfServicePath> sfServicePathList_all = SfcProviderServiceFunctionAPI.readServiceFunctionState(oldSfName);
         List <SfServicePath> sfServicePathList = new ArrayList<>();
         List <RspName> RspList = new ArrayList<>();
         List <RenderedServicePath> renderedServicePathList = new ArrayList<>();
         boolean ret = false;
         int n_backupSf = backupSfNameList.size();
         int n_sfp = sfServicePathList_all.size();

         List <SfName> sfNameList = new ArrayList<>();
         if (n_sfp == 0) {
             LOG.info(" SF : {} is overloading but there is no SFP", oldSfName.getValue(), n_sfp);
         } else {
             LOG.info(" SF : {} and {} SFP are overloading ", oldSfName.getValue(), n_sfp);
             for (int ii = 0 ; ii < n_sfp ; ii++) {
                 sfServicePathList.add (sfServicePathList_all.get(ii));
             }
            LOG.info(" Migration {} RSP of {} RSP", sfServicePathList.size(), n_sfp );
             for (SfServicePath sFPath : sfServicePathList) {
                 SfName backupSfName = backupSfNameList.get(0);

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


                 ret = SfcProviderServiceFunctionAPI.deleteServicePathFromServiceFunctionState(new SfpName (sFPath.getName().getValue()));
                 if (ret == false) {
                     LOG.info("Old RSP is not deleted at SF DB");
                 } else {
                     LOG.info("Old RSP is deleted at SF DB");
                 }
                 ret = SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(rspName);
                 if (ret == false) {
                     LOG.info("Old RSP is not deleted at SFF DB");
                 } else {
                     LOG.info("Old RSP is deleted at SFF DB");
                 }
                ret = SfcProviderRenderedPathAPI.deleteRenderedServicePath(rspName);
                if (ret == false) {
                    LOG.info("Old RSP is not deleted at RSP DB");
                } else {
                    LOG.info("Old RSP is deleted at RSP DB");
                }

                 CreateRenderedPathInputBuilder createRenderedPathInputBuilder = new CreateRenderedPathInputBuilder();
                 createRenderedPathInputBuilder.setName(rspName.getValue()).setParentServiceFunctionPath(renderedServicePath.getParentServiceFunctionPath().getValue());
                 CreateRenderedPathInput createRenderedPathInput = createRenderedPathInputBuilder.build();
                 ServiceFunctionPath serviceFunctionPath = SfcProviderServicePathAPI.readServiceFunctionPath(new SfpName (renderedServicePath.getParentServiceFunctionPath().getValue()));
                 renderedServicePathList.add(SfcLARenderedPathAPI.createFailoverRenderedServicePathAndState (serviceFunctionPath, createRenderedPathInput , sfNameList));
             }
         }


   }
}


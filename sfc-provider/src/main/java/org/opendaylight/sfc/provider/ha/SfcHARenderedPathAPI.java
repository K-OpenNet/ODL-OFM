/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.provider.ha;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.sfc.provider.api.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.CreateRenderedPathInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.RenderedServicePaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.create.rendered.path.input.ContextHeaderAllocationType1;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.create.rendered.path.input.context.header.allocation.type._1.VxlanClassifier;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePath;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.RenderedServicePathKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHop;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.rsp.rev140701.rendered.service.paths.rendered.service.path.RenderedServicePathHopBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfc.rev140701.service.function.chain.grouping.ServiceFunctionChain;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Nsh;
//import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.Transport;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sl.rev140701.VxlanGpe;
import org.opendaylight.yang.gen.v1.urn.intel.params.xml.ns.yang.sfc.sfst.rev150312.*;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStart;
import static org.opendaylight.sfc.provider.SfcProviderDebug.printTraceStop;

/**
 * This class has the APIs to operate on the Rendered path datastore.
 * <p>
 * It is normally called from onDataChanged() through a executor
 * service. We need to use an executor service because we can not
 * operate on a datastore while on onDataChanged() context.
 *
 * @author Jaewook Lee (iioiioiio12345@gmail.com)
 * @version 0.1
 *
 * @since 2017-02-04
 */
public class SfcHARenderedPathAPI {

    private static final Logger LOG = LoggerFactory.getLogger(SfcHARenderedPathAPI.class);
    private static final int MAX_STARTING_INDEX = 255;

    /**
     * Creates a RSP and all the associated operational state based on the
     * given service function path and sfNameList
     * <p>
     *
     * @param createdServiceFunctionPath Service Function Path
     * @param createRenderedPathInput CreateRenderedPathInput object
     * @param sfNameList ServiceFunctions object
     * @return RenderedServicePath Created RSP or null
     */
    public static RenderedServicePath createFailoverRenderedServicePathAndState(ServiceFunctionPath createdServiceFunctionPath,
            CreateRenderedPathInput createRenderedPathInput, List<SfName> sfNameList) {
        RenderedServicePath renderedServicePath;

        boolean rspSuccessful = false;
        boolean addPathToSffStateSuccessful = false;
        boolean addPathToSfStateSuccessful = false;


        // Create RSP
        if ((renderedServicePath = SfcHARenderedPathAPI.createfailoverRenderedServicePathEntry(createdServiceFunctionPath,
                createRenderedPathInput, sfNameList)) != null) {
            rspSuccessful = true;

        } else {
            LOG.error("Could not create RSP. System state inconsistent. Deleting and add SFP {} back",
                    createdServiceFunctionPath.getName());
        }

        // Add Path name to SFF operational state
        if (rspSuccessful && SfcProviderServiceForwarderAPI.addPathToServiceForwarderState(renderedServicePath)) {
            addPathToSffStateSuccessful = true;
        } else {
            if (renderedServicePath != null) {
                SfcProviderRenderedPathAPI.deleteRenderedServicePath(renderedServicePath.getName());
            }
        }

        // Add Path to SF operational state
        if (addPathToSffStateSuccessful
                && SfcProviderServiceFunctionAPI.addPathToServiceFunctionState(renderedServicePath)) {

            addPathToSfStateSuccessful = true;
        } else {
            SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(createdServiceFunctionPath);
            if (renderedServicePath != null) {
                SfcProviderRenderedPathAPI.deleteRenderedServicePath(renderedServicePath.getName());
            }
        }

        // Add RSP to SFP operational state
        if (!(addPathToSfStateSuccessful && SfcProviderServicePathAPI
            .addRenderedPathToServicePathState(createdServiceFunctionPath.getName(), renderedServicePath.getName()))) {
            SfcProviderServiceFunctionAPI
                .deleteServicePathFromServiceFunctionState(createdServiceFunctionPath.getName());
            SfcProviderServiceForwarderAPI.deletePathFromServiceForwarderState(createdServiceFunctionPath);
            if (renderedServicePath != null) {
                SfcProviderRenderedPathAPI.deleteRenderedServicePath(renderedServicePath.getName());
            }
        }

        if (renderedServicePath == null) {
            LOG.error("Failed to create RSP for SFP {}", createdServiceFunctionPath.getName());
        } else {
            LOG.info("Create RSP {} for SFP {} successfully", renderedServicePath.getName(),
                    createdServiceFunctionPath.getName());
        }

        return renderedServicePath;
    }

    /**
     * Given a list of Service Functions, create a RenderedServicePath Hop List
     *
     * @param serviceFunctionNameList List of ServiceFunctions
     * @param sfgNameList List of ServiceFunctionGroups
     * @param serviceIndex Starting index
     * @return List of {@link RenderedServicePathHop}
     */
    protected static List<RenderedServicePathHop> createFailoverRenderedServicePathHopList(List<SfName> serviceFunctionNameList,
            List<String> sfgNameList, int serviceIndex) {
        List<RenderedServicePathHop> renderedServicePathHopArrayList = new ArrayList<>();
        RenderedServicePathHopBuilder renderedServicePathHopBuilder = new RenderedServicePathHopBuilder();

        short posIndex = 0;

        if (serviceFunctionNameList == null && sfgNameList == null) {
            LOG.error("Could not create the hop list caused by empty name list");
            return null;
        }

         if (sfgNameList == null) {
            for (SfName serviceFunctionName : serviceFunctionNameList) {
                ServiceFunction serviceFunction =
                        SfcProviderServiceFunctionAPI.readServiceFunction(serviceFunctionName);
                if (serviceFunction == null) {
                    LOG.error("Could not find suitable SF in data store by name: {}", serviceFunctionName);
                    return null;
                }
                createFailoverSFHopBuilder(serviceIndex, renderedServicePathHopBuilder, posIndex, serviceFunctionName,
                        serviceFunction);
                renderedServicePathHopArrayList.add(posIndex, renderedServicePathHopBuilder.build());
                serviceIndex--;
                posIndex++;
            }
        }

        return renderedServicePathHopArrayList;
    }

    /**
     * Create a failover Rendered Path and all the associated operational state based on the
     * given rendered service path and sflist
     * <p>
     *
     * @param serviceFunctionPath RSP Object
     * @param createRenderedPathInput CreateRenderedPathInput object
     * @param sfNameList SfcServiceFunctionSchedulerAPI object
     * @return RenderedServicePath
     */
    protected static RenderedServicePath createfailoverRenderedServicePathEntry(ServiceFunctionPath serviceFunctionPath,
            CreateRenderedPathInput createRenderedPathInput, List<SfName> sfNameList) {

        printTraceStart(LOG);

        long pathId;
        int serviceIndex;
        RenderedServicePath ret = null;

        // Provisional code to test new RPC parameters

        ContextHeaderAllocationType1 contextHeaderAllocationType1 =
                createRenderedPathInput.getContextHeaderAllocationType1();
        if (contextHeaderAllocationType1 != null) {
            Class<? extends DataContainer> contextHeaderAllocationType1ImplementedInterface =
                    contextHeaderAllocationType1.getImplementedInterface();
            if (contextHeaderAllocationType1ImplementedInterface.equals(VxlanClassifier.class)) {
                LOG.debug("ok");
            }
        }
        // String simplectxName = contextHeaderAllocationType1ImplementedInterface.getSimpleName();
        // simplectxName is VxlanClassifier
        ServiceFunctionChain serviceFunctionChain;
        SfcName serviceFunctionChainName = serviceFunctionPath.getServiceChainName();
        serviceFunctionChain = serviceFunctionChainName != null ? SfcProviderServiceChainAPI
            .readServiceFunctionChain(serviceFunctionChainName) : null;
        if (serviceFunctionChain == null) {
            LOG.error("ServiceFunctionChain name for Path {} not provided", serviceFunctionPath.getName());
            return null;
        }

        RenderedServicePathBuilder renderedServicePathBuilder = new RenderedServicePathBuilder();

        // Descending order
        serviceIndex = MAX_STARTING_INDEX;

        List<String> sfgNameList = null;

        if (sfNameList == null) {
            LOG.warn("createRenderedServicePathEntry scheduler.scheduleServiceFunctions() returned null list");
            return null;
        }
        List<RenderedServicePathHop> renderedServicePathHopArrayList =
                createFailoverRenderedServicePathHopList(sfNameList, sfgNameList, serviceIndex);

        if (renderedServicePathHopArrayList == null) {
            LOG.warn("createRenderedServicePathEntry createFailoverRenderedServicePathHopList returned null list");
            return null;
        }

        // Build the service function path so it can be committed to datastore
        /*
         * pathId = (serviceFunctionPath.getPathId() != null) ?
         * serviceFunctionPath.getPathId() :
         * numCreatedPathIncrementGet();
         */

        if (serviceFunctionPath.getPathId() == null) {
            pathId = SfcServicePathId.check_and_allocate_pathid();
        } else {
            pathId = SfcServicePathId.check_and_allocate_pathid(serviceFunctionPath.getPathId());
        }

        if (pathId == -1) {
            LOG.error("{}: Failed to allocate path-id: {}", Thread.currentThread().getStackTrace()[1], pathId);
            return null;
        }

        renderedServicePathBuilder.setRenderedServicePathHop(renderedServicePathHopArrayList);
        // TODO Bug 4495 - RPCs hiding heuristics using Strings - alagalah
        if (createRenderedPathInput.getName() == null || createRenderedPathInput.getName().isEmpty()) {
            if (serviceFunctionPath.getName() != null) {
                renderedServicePathBuilder
                    .setName(new RspName(serviceFunctionPath.getName().getValue() + "-Path-" + pathId));
            } else {
                LOG.error("{}: Failed to set RSP Name as it was null and SFP Name was null.",
                        Thread.currentThread().getStackTrace()[1]);
                return null;
            }
        } else {
            renderedServicePathBuilder.setName(new RspName(createRenderedPathInput.getName()));

        }

        renderedServicePathBuilder.setPathId(pathId);
        // TODO: Find out the exact rules for service index generation
        // renderedServicePathBuilder.setStartingIndex((short)
        // renderedServicePathHopArrayList.size());
        renderedServicePathBuilder.setStartingIndex((short) MAX_STARTING_INDEX);
        renderedServicePathBuilder.setServiceChainName(serviceFunctionChainName);
        renderedServicePathBuilder.setParentServiceFunctionPath(serviceFunctionPath.getName());
        renderedServicePathBuilder.setContextMetadata(serviceFunctionPath.getContextMetadata());
        renderedServicePathBuilder.setVariableMetadata(serviceFunctionPath.getVariableMetadata());

        if (serviceFunctionPath.getTransportType() == null) {
            // TODO this is a temporary workaround to a YANG problem
            // Even though the SFP.transportType is defined with a default, if its not
            // specified in the configuration, it can still return null
            renderedServicePathBuilder.setTransportType(VxlanGpe.class);
        } else {
            renderedServicePathBuilder.setTransportType(serviceFunctionPath.getTransportType());
        }

        // If no encapsulation type specified, default is NSH for VxlanGpe and Transport
        // in any other case
//        renderedServicePathBuilder.setSfcEncapsulation(VxlanGpe.class);
//                serviceFunctionPath.getSfcEncapsulation() != null ?
//                        serviceFunctionPath.getSfcEncapsulation() :
//                        renderedServicePathBuilder.getTransportType().equals(VxlanGpe.class);
//                                Nsh.class :
//                                Transport.class);

        RenderedServicePathKey renderedServicePathKey =
                new RenderedServicePathKey(renderedServicePathBuilder.getName());
        InstanceIdentifier<RenderedServicePath> rspIID;
        rspIID = InstanceIdentifier.builder(RenderedServicePaths.class)
            .child(RenderedServicePath.class, renderedServicePathKey)
            .build();

        RenderedServicePath renderedServicePath = renderedServicePathBuilder.build();

        if (SfcDataStoreAPI.writeMergeTransactionAPI(rspIID, renderedServicePath, LogicalDatastoreType.OPERATIONAL)) {
            ret = renderedServicePath;
        } else {
            LOG.error("{}: Failed to create Rendered Service Path: {}", Thread.currentThread().getStackTrace()[1],
                    serviceFunctionPath.getName());
        }
        printTraceStop(LOG);
        return ret;
    }

    private static void createFailoverSFHopBuilder(int serviceIndex,
            RenderedServicePathHopBuilder renderedServicePathHopBuilder, short posIndex, SfName serviceFunctionName,
            ServiceFunction serviceFunction) {
        createFailoverHopBuilderInternal(serviceIndex, renderedServicePathHopBuilder, posIndex, serviceFunction);
        renderedServicePathHopBuilder.setServiceFunctionName(serviceFunctionName);
    }


    private static void createFailoverHopBuilderInternal(int serviceIndex,
            RenderedServicePathHopBuilder renderedServicePathHopBuilder, short posIndex,
            ServiceFunction serviceFunction) {
        SffName serviceFunctionForwarderName =
                serviceFunction.getSfDataPlaneLocator().get(0).getServiceFunctionForwarder();

        ServiceFunctionForwarder serviceFunctionForwarder =
                SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(serviceFunctionForwarderName);
        if (serviceFunctionForwarder != null && serviceFunctionForwarder.getSffDataPlaneLocator() != null
                && serviceFunctionForwarder.getSffDataPlaneLocator().get(0) != null) {
            renderedServicePathHopBuilder
                .setServiceFunctionForwarderLocator(serviceFunctionForwarder.getSffDataPlaneLocator().get(0).getName());
        }

        renderedServicePathHopBuilder.setHopNumber(posIndex)
            .setServiceIndex((short) serviceIndex)
            .setServiceFunctionForwarder(serviceFunctionForwarderName);
    }

}

/*
 * Copyright 2017-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.foo;

import com.google.common.collect.ImmutableList;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.floodlightpof.protocol.OFMatch20;
import org.onosproject.floodlightpof.protocol.OFPacketOut;
import org.onosproject.floodlightpof.protocol.action.OFAction;
import org.onosproject.floodlightpof.protocol.table.OFFlowTable;
import org.onosproject.floodlightpof.protocol.table.OFTableType;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.DefaultPofActions;
import org.onosproject.net.flow.instructions.DefaultPofInstructions;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.group.*;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.table.DefaultFlowTable;
import org.onosproject.net.table.FlowTable;
import org.onosproject.net.table.FlowTableId;
import org.onosproject.net.table.FlowTableStore;
import org.onosproject.openflow.controller.DefaultOpenFlowPacketContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.net.table.FlowTableService;
import org.onosproject.net.flow.FlowRuleService;
import com.google.common.base.Objects;
import javax.xml.soap.Node;
import java.util.*;
import org.jboss.netty.channel.Channel;
import com.google.common.base.*;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true)
public class AppComponent {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowTableStore tableStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowTableService flowTableService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceAdminService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    private ApplicationId AppId;
    private ReactivePacketProcessor processor = new ReactivePacketProcessor();
    private final Logger log = LoggerFactory.getLogger(getClass());
    private NodeId local;
    private DeviceId deviceId;
    private int tableId;
    private NodeId master;
    private Channel channel;

    @Activate
    protected void activate() throws InterruptedException {
        AppId = coreService.registerApplication("org.foo.app");
        log.info("Started");
        local = clusterService.getLocalNode().id();
        Iterable<Device> devices = deviceService.getAvailableDevices();
        Iterator<Device> deviceIterator=devices.iterator();
        while (deviceIterator.hasNext()){
            deviceId = deviceIterator.next().id();
        }
        log.info("deviceId is {}",deviceId);
        if(deviceId != null){
            master = mastershipService.getMasterFor(deviceId);

        }
        if (Objects.equal(master, local)) {
            log.info("equal");
            log.info("deviceId is {}",deviceId);
            List<Port> portList = deviceService.getPorts(deviceId);

            for (Port port : portList) {
                log.info("port in portList:" + port.toString());
                deviceService.changePortState(deviceId, port.number(), true);
            }
           // tableId= sendPofFlowTable(deviceId);


//            Timer timer = new Timer();
//            TimerTask timerTask = new TimerTask() {
//                @Override
//                public void run() {
//                   return ;
//                }
//            };
//            timer.schedule(timerTask,10);

           // sendPofFlowRule(deviceId,tableId);
        }
       // packetService.addProcessor(processor, PacketProcessor.director(2));
//        if(local.equals(master)){
//
//        }
        log.info("sendGroupMod");
        sendGroupMod();


    }


    private int sendPofFlowTable(DeviceId deviceId) {

        byte smallTableId;
        tableId = tableStore.getNewGlobalFlowTableId(deviceId, OFTableType.OF_MM_TABLE);
        OFMatch20 srcIP = new OFMatch20();
        srcIP.setFieldId((short) 1);
        srcIP.setFieldName("srcIp");
        srcIP.setOffset((short) 208);
        srcIP.setLength((short) 32);

        OFMatch20 dstIP = new OFMatch20();
        dstIP.setFieldId((short) 2);
        dstIP.setFieldName("dstIp");
        dstIP.setOffset((short) 240);
        dstIP.setLength((short) 32);

        ArrayList<OFMatch20> match20List = new ArrayList<OFMatch20>();
        match20List.add(srcIP);
        match20List.add(dstIP);
        OFFlowTable ofFlowTable = new OFFlowTable();
        ofFlowTable.setTableId((byte)tableId);
        ofFlowTable.setTableName("FirstEntryTable");
        ofFlowTable.setTableSize(64);
        ofFlowTable.setTableType(OFTableType.OF_MM_TABLE);
        ofFlowTable.setMatchFieldList(match20List);
        ofFlowTable.setMatchFieldNum((byte) match20List.size());
        ofFlowTable.setCommand(null);
        ofFlowTable.setKeyLength((short) 64);
        log.info("++++ before build flowtable:" + AppId);
        FlowTable flowTable = DefaultFlowTable.builder()
                .withFlowTable(ofFlowTable)
                .forTable(tableId)
                .forDevice(deviceId)
                .fromApp(AppId)
                .build();
        log.info("++++:" + flowTable.toString());
        log.info("++++ before applyFlowTables");
        flowTableService.applyFlowTables(flowTable);
        log.info("++++ send flow table successfully");
        return tableId;
    }
    private void sendGroupMod() {
        log.info("begin sendGroupMod");
        GroupId groupId = new GroupId(123);
        byte[] keyData = "abc".getBytes();
        final GroupKey groupKey = new DefaultGroupKey(keyData);
        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
        List<OFAction> actions = new ArrayList<OFAction>();
        int outPort=0;
        actions.add(DefaultPofActions.output((short) 0, (short) 0, (short) 0, outPort).action());

        builder.add(DefaultPofInstructions.applyActions(actions));

        short weight = 5;
        GroupBucket bucket = DefaultGroupBucket.createSelectGroupBucket(builder.build(), weight);
        GroupBuckets groupBuckets = new GroupBuckets(ImmutableList.of(bucket));
        DefaultGroupDescription groupDescription = new DefaultGroupDescription(deviceId,
                GroupDescription.Type.SELECT,
                groupBuckets, groupKey, groupId.id(), AppId);
        log.info("app groupDescription : {}", groupDescription.toString());
        groupService.addGroup(groupDescription);

    }
    private void sendPofFlowRule(DeviceId deviceId,int tableId){
        log.info("tableId : {}", tableId);
        int newFlowEntryId=tableStore.getNewFlowEntryId(deviceId,tableId);
        log.info("++++ newFlowEntryId; {}",newFlowEntryId);
        log.info("@niubin starting building flowrule");
        int srcIp4Address= Ip4Address.valueOf("10.0.0.1").toInt();
        String srcToHex=Integer.toHexString(srcIp4Address);
        if(srcToHex.length()!=8) {
            String str=new String("0");
            srcToHex=str.concat(srcToHex);
        }
        int dstIp4Address=Ip4Address.valueOf("10.0.0.2").toInt();
        String dstToHex=Integer.toHexString(dstIp4Address);
        if(dstToHex.length()!=8) {
            String str=new String("0");
            dstToHex=str.concat(dstToHex);
        }
        TrafficSelector.Builder pbuilder = DefaultTrafficSelector.builder();
        ArrayList<Criterion> entryList = new ArrayList<Criterion>();
        entryList.add(Criteria.matchOffsetLength((short) 1, (short) 208, (short) 32, srcToHex, "ffffffff"));
        entryList.add(Criteria.matchOffsetLength((short) 2, (short) 240, (short) 32, dstToHex, "ffffffff"));
        pbuilder.add(Criteria.matchOffsetLength(entryList));
        log.info("++++pbuilder: {}" + pbuilder.toString());
        TrafficTreatment.Builder ppbuilder = DefaultTrafficTreatment.builder();
        List<OFAction> actions = new ArrayList<OFAction>();
        int outPort=0;
        actions.add(DefaultPofActions.output((short) 0, (short) 0, (short) 0, outPort).action());
        ppbuilder.add(DefaultPofInstructions.applyActions(actions));
        log.info("++++ppbuilder: {}" + ppbuilder.toString());
        TrafficSelector selector = pbuilder.build();
        TrafficTreatment treatment = ppbuilder.build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .forTable(tableId)
                .forDevice(deviceId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(1)
                .makePermanent()
                .withCookie(newFlowEntryId)
                .build();

        log.info("++++flow rule: {}", flowRule.toString());
        flowRuleService.applyFlowRules(flowRule);

    }

    @Deactivate
    protected void deactivate() {
        flowTableService.removeFlowTablesByTableId(deviceId,new FlowTableId(0));
        packetService.removeProcessor(processor);
        processor = null;
        log.info("Stopped");
    }
    private class ReactivePacketProcessor implements PacketProcessor{
        @Override
        public void process(PacketContext packetContext) {
          //  log.info("get the packet");
//            if (packetContext.isHandled()) {
//                log.info("packetContext isHandled");
//                return;
//            }
            InboundPacket pkt = packetContext.inPacket();
            Ethernet ethpkt = pkt.parsed();
           // log.info("packet in successfully");
            packetOut(packetContext, PortNumber.FLOOD);
           // packetOut(packetContext, PortNumber.portNumber(1));
            //sendPofFlowRule(deviceId,tableId);
        }

        private void packetOut(PacketContext packetContext, PortNumber portNumber) {
            //log.info("packet out begin");
            //List<OFAction>actions=new ArrayList<>();
           // actions.add(DefaultPofActions.output((short)0,(short)0,(short)0,(int)portNumber.toLong()).action());
            //packetContext.treatmentBuilder().add(DefaultPofInstructions.applyActions(actions));

//            OFPacketOut pktout = null;
            packetContext.treatmentBuilder().setOutput(portNumber);
            packetContext.send();
           // log.info("packet out successfully");
        }
    }
}

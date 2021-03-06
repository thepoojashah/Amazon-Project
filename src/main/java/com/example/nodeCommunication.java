package com.example;

/*
 * #%L
 * myHellopApp
 * %%
 * Copyright (C) 2012 - 2018 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.Address;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestHandler;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.util.RspList;
import org.jgroups.util.Util;

import java.util.Iterator;
import java.util.Set;

public class nodeCommunication extends ReceiverAdapter implements RequestHandler {

    JChannel channel;

    MessageDispatcher disp;

    RspList rsp_list;

    RequestOptions options = new RequestOptions(ResponseMode.GET_FIRST, 60000).setTransientFlags(org.jgroups.Message.TransientFlag.DONT_LOOPBACK).SYNC();

    public static void main(String[] args) throws Exception {
        new nodeCommunication().start();
    }

    public void start() throws Exception {
        channel = new JChannel().setDiscardOwnMessages(true);
        channel.setReceiver(this);
        channel.connect("ChatCluster");
        disp = new MessageDispatcher(channel, this);
    }

    public void putHandling(String queue, String message, String messageID) {
        try {
            String line = "";
            line = "put:" + queue + ":" + message + ":" + messageID;
            byte[] toSend = Util.stringToBytes(line);
            rsp_list = disp.castMessage(null, toSend, 0, toSend.length, options);
            Set set = rsp_list.keySet();
            Iterator<Address> iter = set.iterator();
            Address extra = channel.getAddress();
            while (iter.hasNext()) {
                extra = iter.next();
            }
            System.out.println(extra);
            line = "deleteExtra:" + queue + ":" + messageID;
            byte[] send = Util.stringToBytes(line);
            disp.sendMessage(extra, send, 0, send.length, options);
            System.out.println(line);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void getHandling(String queue, String messageID) {
        try {
            String line = "";
            line = "get:" + queue + ":" + messageID;
            System.out.println(line);
            byte[] msg = Util.stringToBytes(line);
            rsp_list = disp.castMessage(null, msg, 0, msg.length, options);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void deleteHandling(String queue, String messageID) {
        try {
            String line = "";
            line = "delete:" + queue + ":" + messageID;
            byte[] msg = Util.stringToBytes(line);
            rsp_list = disp.castMessage(null, msg, 0, msg.length, options);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void viewAccepted(View new_view) {
        System.out.println("** view: " + new_view);
    }

    @Override
    public String handle(Message msg) throws Exception {

        String rec = Util.bytesToString(msg.getBuffer());
        String[] receivedMessage = rec.split(":");
        String response = "";
        if (receivedMessage[0].equals("put")) {

            String queue_name = receivedMessage[1];
            String message = receivedMessage[2];
            String messageID = receivedMessage[3];
            PutHandler putHandler = new PutHandler();
            response = putHandler.doPut(App.mainHashMap.get(queue_name), message, messageID);

        } else if (receivedMessage[0].equals("get")) {

            String queue_name = receivedMessage[1];
            String messageID = receivedMessage[2];
            System.out.println("get");
            GetHandler getHandler = new GetHandler();
            response = getHandler.doGetCluster(App.mainHashMap.get(queue_name), messageID);

        } else if (receivedMessage[0].equals("delete")) {

            String queue_name = receivedMessage[1];
            String messageID = receivedMessage[2];
            DeleteHandler deleteHandler = new DeleteHandler();
            response = deleteHandler.doDelete(App.mainHashMap.get(queue_name), messageID);

        } else if (receivedMessage[0].equals("deleteExtra")) {
            GetHandler get = new GetHandler();
            response = get.doGet(App.mainHashMap.get(receivedMessage[1]), receivedMessage[2]);
            System.out.println(response);
        }
        return response;
    }

}

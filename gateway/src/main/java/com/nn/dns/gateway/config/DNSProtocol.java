package com.nn.dns.gateway.config;

import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.Random;

@Component
public class DNSProtocol {

    private int randomInt(){
        Random rand = new Random();
        return rand.nextInt();
    }

    private byte[] builtDNSPacketHeader() {
        /*
         * 构造DNS数据包包头,总共12字节
         */
        byte[] header = new byte[12];
        ByteBuffer buffer = ByteBuffer.wrap(header);
        //2字节的会话id
        buffer.putShort((short)randomInt());
        //接下来是2字节的操作码，不同的比特位有相应含义
        short opCode = 0;
        /*
         * 如果是查询数据包，第0个比特位要将最低位设置为0,接下来的4个比特位表示查询类型，如果是查询ip则设置为0，
         * 第5个比特位由服务器在回复数据包中设置，用于表明信息是它拥有的还是从其他服务器查询而来，
         * 第6个比特位表示消息是否有分割，有的话设置为1，由于我们使用UDP，因此消息不会有分割。
         * 第7个比特位表示是否使用递归式查询请求，我们设置成1表示使用递归式查询,
         * 第8个比特位由服务器返回时设置，表示它是否接受递归式查询
         * 第9，10，11，3个比特位必须保留为0，
         * 最后四个比特由服务器回复数据包设置，0表示正常返回数据，1表示请求数据格式错误，2表示服务器出问题，3表示不存在给定域名等等
         * 我们发送数据包时只要将第7个比特位设置成1即可
         */
        opCode = (short) (opCode | (1 << 7));
        buffer.putShort(opCode);
        //接下来是2字节的question count,由于我们只有1个请求，因此它设置成1
        short questionCount = 1;
        buffer.putShort(questionCount);
        //剩下的默认设置成0
        short answerRRCount = 0;
        buffer.putShort(answerRRCount);
        short authorityRRCount = 0;
        buffer.putShort(authorityRRCount);
        short additionalRRCount = 0;
        buffer.putShort(additionalRRCount);
        return buffer.array();
    }

    private byte[] builtDNSPacketQuestion(String domainName) {
        /*
         * 构造DNS数据包中包含域名的查询数据结构
         * 首先是要查询的域名，它的结构是是：字符个数+是对应字符，
         * 例如域名字符串pan.baidu.com对应的内容为
         * 3pan[5]baidu[3]com也就是把‘.'换成它后面跟着的字母个数
         */
        //根据.将域名分割成多个部分,第一个1用于记录"pan"的长度，第二个1用0表示字符串结束 QUESTION_TYPE_LENGTH:2 QUESTION_CLASS_LENGTH:2
        byte[] dnsQuestion = new byte[1 + 1 + domainName.length() + 2 + 2];
        String[] domainParts = domainName.split("\\.");
        ByteBuffer buffer = ByteBuffer.wrap(dnsQuestion);
        for (int i = 0; i < domainParts.length; i++) {
            //先填写字符个数
            buffer.put((byte)domainParts[i].length());
            //填写字符
            for(int k = 0; k < domainParts[i].length(); k++) {
                buffer.put((byte) domainParts[i].charAt(k));
            }
        }
        //表示域名字符串结束
        byte end = 0;
        buffer.put(end);
        //填写查询问题的类型和级别
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        return dnsQuestion;
    }

    public byte[] getDnsPacketBuffer(String domainName) {

        byte[] dnsPacketHeader = builtDNSPacketHeader();
        byte[] packetQuestion = builtDNSPacketQuestion(domainName);

        //向服务器发送域名查询请求数据包
        byte[] dnsPacketBuffer = new byte[dnsPacketHeader.length + packetQuestion.length];
        ByteBuffer buffer = ByteBuffer.wrap(dnsPacketBuffer);
        buffer.put(dnsPacketHeader);
        buffer.put(packetQuestion);

        return dnsPacketBuffer;

    }

}
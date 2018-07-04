package com.example.config;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.context.annotation.Bean;

import java.io.IOException;


/**
 * @author allen
 */
@org.springframework.context.annotation.Configuration
public class Hbase {
    @Bean
    public Connection getHbaseConnect() throws IOException {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "myhbase");
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        conf.set("log4j.logger.org.apache.hadoop.hbase", "WARN");
        Connection connection = ConnectionFactory.createConnection(conf);
        return connection;
    }

    @Bean
    public Admin getHbaseAdmin(Connection connection) throws IOException{
        Admin admin = connection.getAdmin();
        return admin;
    }

}
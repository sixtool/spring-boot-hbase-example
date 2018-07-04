# spring-boot-hbase-example
1，搭建hbase环境   
      废话不多说，直接上docker镜像安装，运行，hbase环境搭建完毕，这里就不细写hbase环境如何搭建，docker是个好东西，下面来介绍docker开启hbase环境的
      这里安装启动docker省去，直接上docker命令
``` sh
#拉取容器
docker pull harisekhon/hbase
#启动容器
docker run -d -h myhbase -p 2181:2181 -p 8080:8080 -p 8085:8085 -p 9090:9090 -p 9095:9095 -p 16000:16000 -p 16010:16010 -p 16201:16201 -p 16301:16301 --name hbase1.3 harisekhon/hbase
```
简单介绍上述命令参数如下：

-d表示后台 
-h 定义容器host 
-p表示端口映射 
–name 表示容器别名 (我这里命名hbase1.3) 
harisekhon/hbase是image镜像

2，修改host文件，等会开发要使用域名
``` sh
vim /etc/hosts
#添加
127.0.0.1 myhbase 
```
3，直接访问页面http://localhost:16010/master-status，看是否正常
![image](https://github.com/tanwenliang/attachment/blob/master/spring-boot-hbase-example/image2018-7-4%2013_54_55.png.jpeg)
4，进入容器玩一玩hbase shell 命令
#进入容器
docker exec -it hbase1.3 /bin/bash
#执行hbase命令
hbase shell
看如下图，哈哈，成功
![image](https://github.com/tanwenliang/attachment/blob/master/spring-boot-hbase-example/image2018-7-4%2013_56_16.png.jpeg)
 可以看到hbase正常启动

5，上spring boot 代码测试，连接hbase做 curd操作 演示

项目结构如下：
![image](https://github.com/tanwenliang/attachment/blob/master/spring-boot-hbase-example/image2018-7-4%2013_57_51.png.jpeg)
 

配置类
``` java
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
        Connection connection = ConnectionFactory.createConnection(conf);  //这里连接不考虑性能问题
        return connection;
    }

    @Bean
    public Admin getHbaseAdmin(Connection connection) throws IOException{
        Admin admin = connection.getAdmin();
        return admin;
    }

}
```
controller类

``` java
 package com.example.controller;

import com.example.config.Hbase;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
public class HelloController {
    @Autowired
    Hbase hbase;

    static final String TABLE_NAME = "box_device";


    @RequestMapping("/saveDeviceInfoToHbase")
    public Object saveDeviceInfoToHbase(@RequestParam(required = true) String model,
                                        @RequestParam(required = true) String chip,
                                        @RequestParam(required = true) String mac,
                                        @RequestParam(required = true) String emmcId,
                                        @RequestParam(required = true) String barcode,
                                        @RequestParam(required = true) String tcVersion,
                                        @RequestParam(required = true) String systemVersion
    ) throws Exception {
        String rowKey = DigestUtils.md5Hex(model + chip + mac + barcode + emmcId);
        this.updateTable(rowKey, "model", "model", model);
        this.updateTable(rowKey, "chip", "chip", chip);
        this.updateTable(rowKey, "mac", "mac", mac);
        this.updateTable(rowKey, "emmcId", "emmcId", emmcId);
        this.updateTable(rowKey, "barcode", "barcode", barcode);
        this.updateTable(rowKey, "tcVersion", "mac", tcVersion);
        this.updateTable(rowKey, "systemVersion", "systemVersion", systemVersion);
        HashMap<Object, Object> data = new HashMap<>();
        data.put("serviceId", rowKey);
        return data;
    }


    @RequestMapping("/getDeviceInfoHbase")
    public Object getInfoHbase(@RequestParam(required = true) String sid
    ) throws IOException {
        Connection hbaseConnect = hbase.getHbaseConnect();
        Table table = hbaseConnect.getTable(TableName.valueOf(TABLE_NAME));
        Get get = new Get(Bytes.toBytes(sid));
        get.addColumn(Bytes.toBytes("model"), Bytes.toBytes("model"));
        get.addColumn(Bytes.toBytes("chip"), Bytes.toBytes("chip"));
        get.addColumn(Bytes.toBytes("mac"), Bytes.toBytes("mac"));
        get.addColumn(Bytes.toBytes("emmcId"), Bytes.toBytes("emmcId"));
        get.addColumn(Bytes.toBytes("barcode"), Bytes.toBytes("barcode"));
        get.addColumn(Bytes.toBytes("tcVersion"), Bytes.toBytes("tcVersion"));
        get.addColumn(Bytes.toBytes("systemVersion"), Bytes.toBytes("systemVersion"));
        get.setMaxVersions(30);
        Result result = table.get(get);
        ArrayList<Object> objects = new ArrayList<>();
        if (result.listCells() != null) {
            for (Cell cell : result.listCells()) {
                HashMap<Object, Object> data = new HashMap<>();
                data.put("family", Bytes.toString(cell.getFamilyArray(), cell.getFamilyOffset(), cell.getFamilyLength()));
                data.put("qualifier", Bytes.toString(cell.getQualifierArray(), cell.getQualifierOffset(), cell.getQualifierLength()));
                data.put("value", Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength()));
                data.put("Timestamp", cell.getTimestamp());
                objects.add(data);
            }
        } else {
            return objects;
        }

        return objects;
    }


    public void updateTable(String rowKey, String familyName, String columnName, String value) throws Exception {

        Connection hbaseConnect = hbase.getHbaseConnect();
        try {
            Table table = hbaseConnect.getTable(TableName.valueOf(TABLE_NAME));
            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(columnName), Bytes.toBytes(value));
            table.put(put);
            System.out.println("Update table success");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
``` 

5，测试接口
先来报备设备数据，然后服务器生成一个激活id
``` sh
curl -X GET \
'http://localhost:9898/saveDeviceInfoToHbase?model=A55&chip=8218&mac=1ACDD123433&emmcId=00123423423423423423&barcode=A550-82123-231K-2342342&tcVersion=61023423&systemVersion=18234232' \
-H 'Cache-Control: no-cache' \
-H 'Postman-Token: 8448f020-26f1-4a21-a9dc-dc62b37b4b3a'
```
看到结果 如下：
![image](https://github.com/tanwenliang/attachment/blob/master/spring-boot-hbase-example/image2018-7-4%2014_1_33.png.jpeg)


然后通过这个激活id查询设备信息
``` sh
curl -X GET \
'http://localhost:9898/getDeviceInfoHbase?sid=1cac93c999391b345e96adf5ff5d0bae' \
-H 'Cache-Control: no-cache' \
-H 'Postman-Token: 4891060c-3d44-4931-a3b6-9e1e1afce3d9'
```
结果如下：
![image](https://github.com/tanwenliang/attachment/blob/master/spring-boot-hbase-example/image2018-7-4%2014_2_25.png.jpeg)


6，遇到的坑   
1，hbase发现在查询字段，或添加字段时代码太冗余了，字段多写起来非常吃力，找到org.springframework.data.hadoop.hbase.HbaseTemplate 这个类暂未使用，不知道 如何

2，hbase 数据模型灵活，但对数据字段，rowkey, famliy 初期需要考虑清楚，后期是不容易更改的，简单来说可以看成是redis key -value 使用的，所有key前期一定要考虑全面
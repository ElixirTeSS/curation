package yanan.zhang;

import com.mysql.jdbc.Driver;

import com.mysql.jdbc.Driver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Yanan Zhang
 **/
public class MySqlJDBCImpl {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/crawler?useSSL=false&serverTimezone=GMT%2B8";
    private static final String USER_NAME = "root";
    private static final String PASSWORD = "123456";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd");

    /**
     * 获取数据库连接
     *
     * @return
     */
    private Connection getConnection() {
        try {
            // 1.通过DriverManger注册驱动，注意此时Driver是在com.mysql.jdbc包中
            DriverManager.registerDriver(new Driver());
            /**
             * 2.通过DriverManager获取连接对象
             *
             * jdbc:mysql://：这是固定的写法，表示使用jdbc连接mysql数据库
             * localhost：ip地址，本地可以写成localhost。
             * 3306：mysql的端口号。
             * xia：数据库的名字。
             * 第一个root：mysql的用户名
             * 第二个root：mysql的密码。
             *
             */
            return DriverManager.getConnection(DB_URL, USER_NAME, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 删表
     *
     * @return
     */
    public boolean dropTable() {
        String dateStr = SDF.format(new Date());
        String sql = "DROP TABLE IF EXISTS dead_link_records_" + dateStr + ";";
        try {
            //1，得到Connection对象，
            Connection connection = this.getConnection();
            //2，通过Connection获取一个操作sql语句的对象Statement
            if (connection == null) {
                return false;
            }
            Statement statement = connection.createStatement();
            //3，执行删表语句
            statement.execute(sql);
            //4，释放资源
            statement.close();
            connection.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 建表
     *
     * @return
     */
    public boolean createTable() {
        String dateStr = SDF.format(new Date());
        String sql = "CREATE TABLE dead_link_records_" + dateStr +
                "(\n" +
                "    `id`          bigint(20) NOT NULL AUTO_INCREMENT,\n" +
                "    `category`    varchar(50)  NOT NULL COMMENT 'category: events, materials, elearning_materials',\n" +
                "    `page`        int(10) NOT NULL COMMENT 'page number',\n" +
                "    `status_code` int(10) NOT NULL COMMENT 'http status code',\n" +
                "    `reason_phrase` varchar(255) COMMENT 'http reason phrase',\n" +
                "    `type`        varchar(20)  NOT NULL COMMENT 'type: major, minor',\n" +
                "    `dead_link`   varchar(500) NOT NULL COMMENT 'dead link',\n" +
                "    `parent_url`  varchar(500) NOT NULL COMMENT 'parent url',\n" +
                "    `domain_url`  varchar(200) COMMENT 'dead link domain'," +
                "    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',\n" +
                "    PRIMARY KEY (`id`) USING BTREE,\n" +
                "    KEY `idx_domain_url` (`domain_url`) USING BTREE,\n" +
                "    KEY `idx_create_time` (`create_time`) USING BTREE\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='dead link table';\n";
        try {
            //1，得到Connection对象，
            Connection connection = this.getConnection();
            //2，通过Connection获取一个操作sql语句的对象Statement
            if (connection == null) {
                return false;
            }
            Statement statement = connection.createStatement();
            //3，执行建表语句
            statement.execute(sql);
            //4，释放资源
            statement.close();
            connection.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 保存死链数据
     */
    public boolean saveDeadLinkRecord(DeadLinkRecords model) {
        String dateStr = SDF.format(new Date());
        try {
            //1，得到Connection对象，
            Connection connection = this.getConnection();
            //2，通过Connection获取一个操作sql语句的对象Statement
            if (connection == null) {
                return false;
            }
            Statement statement = connection.createStatement();
            //3，获取需要传递的参数
            String category = model.getCategory();
            Integer page = model.getPage();
            Integer statusCode = model.getStatusCode();
            String reasonPhrase = model.getReasonPhrase();
            String type = model.getType();
            String deadLink = model.getDeadLink();
            String parentUrl = model.getParentUrl();
            String domainUrl = model.getDomainUrl();
            //4，写sql语句，参数使用？占位符
            String sql = "INSERT INTO dead_link_records_" + dateStr + "(category, page, status_code, reason_phrase, type, dead_link, parent_url, domain_url) VALUE (?, ?, ?, ?, ?, ?, ?, ?)";
            //5，得到PreparedStatement对象
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            //6，通过PreparedStatement对象设置参数
            preparedStatement.setString(1, category);
            preparedStatement.setInt(2, page);
            preparedStatement.setInt(3, statusCode);
            preparedStatement.setString(4, reasonPhrase);
            preparedStatement.setString(5, type);
            preparedStatement.setString(6, deadLink);
            preparedStatement.setString(7, parentUrl);
            preparedStatement.setString(8, domainUrl);
            //7，执行sql语句
            preparedStatement.execute();
            //8，释放资源
            statement.close();
            connection.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据类别查询死链数据
     *
     * @param category
     * @return
     */
    public List<DeadLinkRecords> selectDeadLinkRecordsByCategory(String category) {
        List<DeadLinkRecords> list = new ArrayList<>();
        String dateStr = SDF.format(new Date());
        try {
            //1，得到Connection对象，
            Connection connection = this.getConnection();
            //2，通过Connection获取一个操作sql语句的对象Statement
            if (connection == null) {
                return list;
            }
            Statement statement = connection.createStatement();
            //3，写sql语句
            String sql = "SELECT * FROM dead_link_records_" + dateStr + " WHERE category = '" + category + "'";
            //4，查询，返回的结果放入ResultSet对象中。
            ResultSet resultSet = statement.executeQuery(sql);
            //5，得到返回的值
            while (resultSet.next()) {
                DeadLinkRecords model = new DeadLinkRecords();
                model.setId(resultSet.getLong(1));
                model.setCategory(resultSet.getString(2));
                model.setPage(resultSet.getInt(3));
                model.setStatusCode(resultSet.getInt(4));
                model.setReasonPhrase(resultSet.getString(5));
                model.setType(resultSet.getString(6));
                model.setDeadLink(resultSet.getString(7));
                model.setParentUrl(resultSet.getString(8));
                model.setDomainUrl(resultSet.getString(9));
                model.setCreateTime(resultSet.getTimestamp(10));
                list.add(model);
            }
            //6，释放资源
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * 根据主域名统计死链数据条数
     *
     * @param domainUrl
     * @return
     */
    public int countDeadLinkRecordsByDomain(String domainUrl) {
        int count = 0;
        String dateStr = SDF.format(new Date());
        try {
            //1，得到Connection对象，
            Connection connection = this.getConnection();
            //2，通过Connection获取一个操作sql语句的对象Statement
            if (connection == null) {
                return 0;
            }
            Statement statement = connection.createStatement();
            //3，写sql语句，参数使用？占位符
            String sql = "SELECT count(*) FROM dead_link_records_" + dateStr + " WHERE domain_url = '" + domainUrl + "'";
            //4，查询，返回的结果放入ResultSet对象中。
            ResultSet resultSet = statement.executeQuery(sql);
            //5，得到返回的值
            while (resultSet.next()) {
                count = resultSet.getInt(1);
            }
            //6，释放资源
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return count;
    }

    /**
     * 删除死链记录
     *
     * @param id
     * @return
     */
    public boolean deleteDeadLinkRecord(long id) {
        String dateStr = SDF.format(new Date());
        try {
            //1，得到Connection对象，
            Connection connection = this.getConnection();
            if (connection == null) {
                return false;
            }
            //2，通过Connection获取一个操作sql语句的对象Statement
            Statement statement = connection.createStatement();
            //3，拼接sql语句
            String sql = "DELETE FROM dead_link_records_" + dateStr + " WHERE id = " + id;
            //4，执行sql语句
            statement.execute(sql);
            //5，释放资源
            statement.close();
            connection.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 建表
     *
     * @return
     */
    public boolean createTableDomain() {
        String dateStr = SDF.format(new Date());
        String sql = "CREATE TABLE dead_link_domain_" + dateStr +
                "(\n" +
                "    `id`            bigint(20) NOT NULL AUTO_INCREMENT,\n" +
                "    `status_code`   int(10) NOT NULL COMMENT 'http status code',\n" +
                "    `reason_phrase` varchar(255) COMMENT 'http reason phrase',\n" +
                "    `domain_url`    varchar(500) NOT NULL COMMENT 'domain url',\n" +
                "    `link_number`   int(10) NOT NULL COMMENT 'number of detected links',\n" +
                "    `create_time`   datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',\n" +
                "    PRIMARY KEY (`id`) USING BTREE,\n" +
                "    KEY `idx_create_time` (`create_time`) USING BTREE\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='dead link domain';";
        try {
            //1，得到Connection对象，
            Connection connection = this.getConnection();
            //2，通过Connection获取一个操作sql语句的对象Statement
            if (connection == null) {
                return false;
            }
            Statement statement = connection.createStatement();
            //3，执行建表语句
            statement.execute(sql);
            //4，释放资源
            statement.close();
            connection.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删表
     *
     * @return
     */
    public boolean dropTableDomain() {
        String dateStr = SDF.format(new Date());
        String sql = "DROP TABLE IF EXISTS dead_link_domain_" + dateStr + ";";
        try {
            //1，得到Connection对象，
            Connection connection = this.getConnection();
            //2，通过Connection获取一个操作sql语句的对象Statement
            if (connection == null) {
                return false;
            }
            Statement statement = connection.createStatement();
            //3，执行删表语句
            statement.execute(sql);
            //4，释放资源
            statement.close();
            connection.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 保存死链数据
     */
    public boolean saveDeadLinkDomain(DeadLinkDomain model) {
        String dateStr = SDF.format(new Date());
        try {
            //1，得到Connection对象，
            Connection connection = this.getConnection();
            //2，通过Connection获取一个操作sql语句的对象Statement
            if (connection == null) {
                return false;
            }
            Statement statement = connection.createStatement();
            //3，获取需要传递的参数
            Integer statusCode = model.getStatusCode();
            String reasonPhrase = model.getReasonPhrase();
            String domainUrl = model.getDomainUrl();
            Integer linkNumber = model.getLinkNumber();
            //4，写sql语句，参数使用？占位符
            String sql = "INSERT INTO dead_link_domain_" + dateStr + "(status_code, reason_phrase, domain_url, link_number) VALUE (?, ?, ?, ?)";
            //5，得到PreparedStatement对象
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            //6，通过PreparedStatement对象设置参数
            preparedStatement.setInt(1, statusCode);
            preparedStatement.setString(2, reasonPhrase);
            preparedStatement.setString(3, domainUrl);
            preparedStatement.setInt(4, linkNumber);
            //7，执行sql语句
            preparedStatement.execute();
            //8，释放资源
            statement.close();
            connection.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 查询主域名数据
     *
     * @return
     */
    public List<DeadLinkDomain> selectDeadLinkDomain() {
        List<DeadLinkDomain> list = new ArrayList<>();
        String dateStr = SDF.format(new Date());
        try {
            //1，得到Connection对象，
            Connection connection = this.getConnection();
            //2，通过Connection获取一个操作sql语句的对象Statement
            if (connection == null) {
                return list;
            }
            Statement statement = connection.createStatement();
            //3，写sql语句
            String sql = "SELECT * FROM dead_link_domain_" + dateStr;
            //4，查询，返回的结果放入ResultSet对象中。
            ResultSet resultSet = statement.executeQuery(sql);
            //5，得到返回的值
            while (resultSet.next()) {
                DeadLinkDomain model = new DeadLinkDomain();
                model.setId(resultSet.getLong(1));
                model.setStatusCode(resultSet.getInt(2));
                model.setReasonPhrase(resultSet.getString(3));
                model.setDomainUrl(resultSet.getString(4));
                model.setLinkNumber(resultSet.getInt(5));
                model.setCreateTime(resultSet.getTimestamp(6));
                list.add(model);
            }
            //6，释放资源
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

}
package yanan.zhang;

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
                "    `reason_phrase` varchar(50) COMMENT 'http reason phrase',\n" +
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
            String deadLink = model.getDeadLink();
            String parentUrl = model.getParentUrl();
            String domainUrl = model.getDomainUrl();
            //4，写sql语句，参数使用？占位符
            String sql = "INSERT INTO dead_link_records_" + dateStr + "(category, page, status_code, reason_phrase, dead_link, parent_url, domain_url) VALUE (?, ?, ?, ?, ?, ?, ?)";
            //5，得到PreparedStatement对象
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            //6，通过PreparedStatement对象设置参数
            preparedStatement.setString(1, category);
            preparedStatement.setInt(2, page);
            preparedStatement.setInt(3, statusCode);
            preparedStatement.setString(4, reasonPhrase);
            preparedStatement.setString(5, deadLink);
            preparedStatement.setString(6, parentUrl);
            preparedStatement.setString(7, domainUrl);
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
     * 查询死链数据
     *
     * @param category
     * @param page
     * @param deadLink
     * @return
     */
    public List<DeadLinkRecords> selectDeadLinkRecords(String category, Integer page, String deadLink) {
        String dateStr = SDF.format(new Date());
        try {
            //1，得到Connection对象，
            Connection connection = this.getConnection();
            //2，通过Connection获取一个操作sql语句的对象Statement
            if (connection == null) {
                return null;
            }
            Statement statement = connection.createStatement();
            //3，写sql语句，参数使用？占位符
            String sql = "SELECT * FROM dead_link_records_" + dateStr + " WHERE category = ? AND page = ? AND dead_link = ?";
            //4，得到PreparedStatement对象
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            //5，通过PreparedStatement对象设置参数
            preparedStatement.setString(1, category);
            preparedStatement.setInt(2, page);
            preparedStatement.setString(3, deadLink);
            //6，查询，返回的结果放入ResultSet对象中。
            ResultSet resultSet = statement.executeQuery(sql);
            // 7.得到返回的值
            List<DeadLinkRecords> resultList = new ArrayList<>();
            while (resultSet.next()) {//resultSet对象可能包含很多行数据，所以要是有while循环。
                long id = resultSet.getLong(1);//第一行的第一列数据，我们知道是id，也知道是long类型，
                String categoryData = resultSet.getString(2);//第二个数据对应category
                int pageData = resultSet.getInt(3);//第三个数据对应page
                int statusCode = resultSet.getInt(4);
                String deadLinkData = resultSet.getString(5);
                String parentUrl = resultSet.getString(6);
                Date createTime = resultSet.getDate(7);
                DeadLinkRecords model = new DeadLinkRecords();
                model.setId(id);
                model.setCategory(categoryData);
                model.setPage(pageData);
                model.setStatusCode(statusCode);
                model.setDeadLink(deadLinkData);
                model.setParentUrl(parentUrl);
                model.setCreateTime(createTime);
                resultList.add(model);
            }
            //6，释放资源
            statement.close();
            connection.close();
            return resultList;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
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
                "    `reason_phrase` varchar(50) COMMENT 'http reason phrase',\n" +
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

}
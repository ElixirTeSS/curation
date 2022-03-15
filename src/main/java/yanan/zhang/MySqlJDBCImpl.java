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
                "    `dead_link_title` varchar(500) COMMENT '死链标题',\n" +
                "    `parent_url`  varchar(500) NOT NULL COMMENT 'parent url',\n" +
                "    `domain_url`  varchar(200) COMMENT 'dead link domain',\n" +
                "    `start`       varchar(100) COMMENT 'start time',\n" +
                "    `end`         varchar(100) COMMENT 'end time',\n" +
                "    `duration`    varchar(10) COMMENT 'duration(day)',\n" +
                "    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',\n" +
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
            String deadLinkTitle = model.getDeadLinkTitle();
            String parentUrl = model.getParentUrl();
            String domainUrl = model.getDomainUrl();
            String start = model.getStart();
            String end = model.getEnd();
            String duration = model.getDuration();
            //4，写sql语句，参数使用？占位符
            String sql = "INSERT INTO dead_link_records_" + dateStr + "(category, page, status_code, reason_phrase, type, dead_link, dead_link_title, parent_url, domain_url, start, end, duration) VALUE (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            //5，得到PreparedStatement对象
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            //6，通过PreparedStatement对象设置参数
            preparedStatement.setString(1, category);
            preparedStatement.setInt(2, page);
            preparedStatement.setInt(3, statusCode);
            preparedStatement.setString(4, reasonPhrase);
            preparedStatement.setString(5, type);
            preparedStatement.setString(6, deadLink);
            preparedStatement.setString(7, deadLinkTitle);
            preparedStatement.setString(8, parentUrl);
            preparedStatement.setString(9, domainUrl);
            preparedStatement.setString(10, start);
            preparedStatement.setString(11, end);
            preparedStatement.setString(12, duration);
            //7，执行sql语句
            preparedStatement.execute();
            //8，释放资源
            preparedStatement.close();
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
                model.setDeadLinkTitle(resultSet.getString(8));
                model.setParentUrl(resultSet.getString(9));
                model.setDomainUrl(resultSet.getString(10));
                model.setStart(resultSet.getString(11));
                model.setEnd(resultSet.getString(12));
                model.setDuration(resultSet.getString(13));
                model.setCreateTime(resultSet.getTimestamp(14));
                list.add(model);
            }
            //6，释放资源
            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * 查询所有死链数据
     *
     * @return
     */
    public List<DeadLinkRecords> selectDeadLinkRecords(String dateStr) {
        List<DeadLinkRecords> list = new ArrayList<>();
        try {
            //1，得到Connection对象，
            Connection connection = this.getConnection();
            //2，通过Connection获取一个操作sql语句的对象Statement
            if (connection == null) {
                return list;
            }
            Statement statement = connection.createStatement();
            //3，写sql语句
            String sql = "SELECT * FROM dead_link_records_" + dateStr;
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
                model.setDeadLinkTitle(resultSet.getString(8));
                model.setParentUrl(resultSet.getString(9));
                model.setDomainUrl(resultSet.getString(10));
                model.setStart(resultSet.getString(11));
                model.setEnd(resultSet.getString(12));
                model.setDuration(resultSet.getString(13));
                model.setCreateTime(resultSet.getTimestamp(14));
                list.add(model);
            }
            //6，释放资源
            resultSet.close();
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
            resultSet.close();
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
                "    `page`          int(10) COMMENT 'page',\n" +
                "    `detail_link`    varchar(500) COMMENT 'detail url',\n" +
                "    `dead_link_title` varchar(500) COMMENT 'dead link title',\n" +
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
            Integer page = model.getPage();
            String detailLink = model.getDetailLink();
            String deadLinkTitle = model.getDeadLinkTitle();
            //4，写sql语句，参数使用？占位符
            String sql = "INSERT INTO dead_link_domain_" + dateStr + "(status_code, reason_phrase, domain_url, link_number, page, detail_link, dead_link_title) VALUE (?, ?, ?, ?, ?, ?, ?)";
            //5，得到PreparedStatement对象
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            //6，通过PreparedStatement对象设置参数
            preparedStatement.setInt(1, statusCode);
            preparedStatement.setString(2, reasonPhrase);
            preparedStatement.setString(3, domainUrl);
            preparedStatement.setInt(4, linkNumber);
            preparedStatement.setInt(5, page);
            preparedStatement.setString(6, detailLink);
            preparedStatement.setString(7, deadLinkTitle);
            //7，执行sql语句
            preparedStatement.execute();
            //8，释放资源
            preparedStatement.close();
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
    public List<DeadLinkDomain> selectDeadLinkDomain(String dateStr) {
        List<DeadLinkDomain> list = new ArrayList<>();
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
                model.setPage(resultSet.getInt(6));
                model.setDetailLink(resultSet.getString(7));
                model.setDeadLinkTitle(resultSet.getString(8));
                model.setCreateTime(resultSet.getTimestamp(9));
                list.add(model);
            }
            //6，释放资源
            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * 查询主域名黑名单数据
     *
     * @return
     */
    public List<BlackListDomain> selectBlackListDomain() {
        List<BlackListDomain> list = new ArrayList<>();
        try {
            //1，得到Connection对象，
            Connection connection = this.getConnection();
            //2，通过Connection获取一个操作sql语句的对象Statement
            if (connection == null) {
                return list;
            }
            Statement statement = connection.createStatement();
            //3，写sql语句
            String sql = "SELECT * FROM black_list_domain";
            //4，查询，返回的结果放入ResultSet对象中。
            ResultSet resultSet = statement.executeQuery(sql);
            //5，得到返回的值
            while (resultSet.next()) {
                BlackListDomain model = new BlackListDomain();
                model.setId(resultSet.getLong(1));
                model.setDomainUrl(resultSet.getString(2));
                model.setCreateTime(resultSet.getTimestamp(3));
                list.add(model);
            }
            //6，释放资源
            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * 保存死链数据
     */
    public boolean saveCollectInfo(CollectInfo model) {
        try {
            //1，得到Connection对象，
            Connection connection = this.getConnection();
            //2，通过Connection获取一个操作sql语句的对象Statement
            if (connection == null) {
                return false;
            }
            Statement statement = connection.createStatement();
            //3，获取需要传递的参数
            int events = model.getEvents();
            int eventsDead = model.getEventsDead();
            int materials = model.getMaterials();
            int materialsDead = model.getMaterialsDead();
            int elearning = model.getElearning();
            int elearningDead = model.getElearningDead();
            int workflows = model.getWorkflows();
            int workflowsDead = model.getWorkflowsDead();
            int domainDead = model.getDomainDead();
            String createDate = model.getCreateDate();
            //4，写sql语句，参数使用？占位符
            String sql = "INSERT INTO collect_info(events, events_dead, materials, materials_dead, elearning, elearning_dead, workflows, workflows_dead, domain_dead, create_date) VALUE (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            //5，得到PreparedStatement对象
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            //6，通过PreparedStatement对象设置参数
            preparedStatement.setInt(1, events);
            preparedStatement.setInt(2, eventsDead);
            preparedStatement.setInt(3, materials);
            preparedStatement.setInt(4, materialsDead);
            preparedStatement.setInt(5, elearning);
            preparedStatement.setInt(6, elearningDead);
            preparedStatement.setInt(7, workflows);
            preparedStatement.setInt(8, workflowsDead);
            preparedStatement.setInt(9, domainDead);
            preparedStatement.setString(10, createDate);
            //7，执行sql语句
            preparedStatement.execute();
            //8，释放资源
            preparedStatement.close();
            statement.close();
            connection.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 查询汇总数据
     *
     * @return
     */
    public List<CollectInfo> selectCollectInfoByDate(List<String> dateList) {
        List<CollectInfo> list = new ArrayList<>();
        try {
            //1，得到Connection对象，
            Connection connection = this.getConnection();
            //2，通过Connection获取一个操作sql语句的对象Statement
            if (connection == null) {
                return list;
            }
            //3，写sql语句
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT * FROM collect_info WHERE create_date IN (");
            for (int i = 0; i < dateList.size(); i++) {
                if ((i == dateList.size() - 1)) {
                    sql.append("?)");
                } else {
                    sql.append("?,");
                }
            }
            PreparedStatement preparedStatement = connection.prepareStatement(sql.toString());
            for (int i = 0; i<dateList.size(); i++) {
                preparedStatement.setString(i + 1, dateList.get(i));
            }
            //4，查询，返回的结果放入ResultSet对象中。
            ResultSet resultSet = preparedStatement.executeQuery();
            //5，得到返回的值
            while (resultSet.next()) {
                CollectInfo model = new CollectInfo();
                model.setId(resultSet.getLong(1));
                model.setEvents(resultSet.getInt(2));
                model.setEventsDead(resultSet.getInt(3));
                model.setMaterials(resultSet.getInt(4));
                model.setMaterialsDead(resultSet.getInt(5));
                model.setElearning(resultSet.getInt(6));
                model.setElearningDead(resultSet.getInt(7));
                model.setWorkflows(resultSet.getInt(8));
                model.setWorkflowsDead(resultSet.getInt(9));
                model.setDomainDead(resultSet.getInt(10));
                model.setCreateDate(resultSet.getString(11));
                list.add(model);
            }
            //6，释放资源
            resultSet.close();
            preparedStatement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

}
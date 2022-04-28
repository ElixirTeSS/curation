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
     * get connection from database
     *
     * @return
     */
    private Connection getConnection() {
        try {
            // 1.register the Driver, here the Driver is in com.mysql.jdbc package
            DriverManager.registerDriver(new Driver());
            /**
             * 2.get connection from DriverManager
             *
             * jdbc:mysql://：use jdbc to connect mysql DB
             * localhost：ip: localhost。
             * 3306
             * 1st root：mysql username
             * 2nd root：mysql password
             */
            return DriverManager.getConnection(DB_URL, USER_NAME, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * delete table
     *
     * @return
     */
    public boolean dropTable() {
        String dateStr = SDF.format(new Date());
        String sql = "DROP TABLE IF EXISTS dead_link_records_" + dateStr + ";";
        try {
            //1，get Connection
            Connection connection = this.getConnection();
            //2，get the statement for precessing sql through Connection
            if (connection == null) {
                return false;
            }
            Statement statement = connection.createStatement();
            //3，execute the deleting
            statement.execute(sql);
            //4，release
            statement.close();
            connection.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * create table
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
                "    `dead_link_title` varchar(500) COMMENT 'title',\n" +
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
            Connection connection = this.getConnection();
            if (connection == null) {
                return false;
            }
            Statement statement = connection.createStatement();
            statement.execute(sql);
            statement.close();
            connection.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * save broken links
     */
    public boolean saveDeadLinkRecord(DeadLinkRecords model) {
        String dateStr = SDF.format(new Date());
        try {
            Connection connection = this.getConnection();
            if (connection == null) {
                return false;
            }
            Statement statement = connection.createStatement();
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
            String sql = "INSERT INTO dead_link_records_" + dateStr + "(category, page, status_code, reason_phrase, type, dead_link, dead_link_title, parent_url, domain_url, start, end, duration) VALUE (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
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
            preparedStatement.execute();
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
     * select broken links by categories
     *
     * @param category
     * @return
     */
    public List<DeadLinkRecords> selectDeadLinkRecordsByCategory(String dateStr, String category) {
        List<DeadLinkRecords> list = new ArrayList<>();
        try {
            Connection connection = this.getConnection();
            if (connection == null) {
                return list;
            }
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM dead_link_records_" + dateStr + " WHERE category = '" + category + "'";
            ResultSet resultSet = statement.executeQuery(sql);
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
     * select all broken links
     *
     * @return
     */
    public List<DeadLinkRecords> selectDeadLinkRecords(String dateStr) {
        List<DeadLinkRecords> list = new ArrayList<>();
        try {
            Connection connection = this.getConnection();
            if (connection == null) {
                return list;
            }
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM dead_link_records_" + dateStr;
            ResultSet resultSet = statement.executeQuery(sql);
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
            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * calculate the amount of the broken links for each broken domain
     *
     * @param domainUrl
     * @return
     */
    public int countDeadLinkRecordsByDomain(String domainUrl) {
        int count = 0;
        String dateStr = SDF.format(new Date());
        try {
            Connection connection = this.getConnection();
            if (connection == null) {
                return 0;
            }
            Statement statement = connection.createStatement();
            String sql = "SELECT count(*) FROM dead_link_records_" + dateStr + " WHERE domain_url = '" + domainUrl + "'";
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                count = resultSet.getInt(1);
            }
            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return count;
    }

    /**
     * calculate the amount of the broken links for each category (exclude black list)
     *
     * @param category
     * @return
     */
    public int countDeadLinkRecordsByCategory(String category) {
        int count = 0;
        String dateStr = SDF.format(new Date());
        try {
            Connection connection = this.getConnection();
            if (connection == null) {
                return 0;
            }
            Statement statement = connection.createStatement();
            //parameter ? (space)
            String sql = "SELECT count(*) FROM dead_link_records_" + dateStr + " WHERE status_code != 1 AND category = '" + category + "'";
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                count = resultSet.getInt(1);
            }
            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return count;
    }

    /**
     * delete broken link record
     *
     * @param id
     * @return
     */
    public boolean deleteDeadLinkRecord(long id) {
        String dateStr = SDF.format(new Date());
        try {
            Connection connection = this.getConnection();
            if (connection == null) {
                return false;
            }
            Statement statement = connection.createStatement();
            String sql = "DELETE FROM dead_link_records_" + dateStr + " WHERE id = " + id;
            statement.execute(sql);
            statement.close();
            connection.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * create broken domain table
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
            Connection connection = this.getConnection();
            if (connection == null) {
                return false;
            }
            Statement statement = connection.createStatement();
            statement.execute(sql);
            statement.close();
            connection.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * delete the broken domain table
     *
     * @return
     */
    public boolean dropTableDomain() {
        String dateStr = SDF.format(new Date());
        String sql = "DROP TABLE IF EXISTS dead_link_domain_" + dateStr + ";";
        try {
            Connection connection = this.getConnection();
            if (connection == null) {
                return false;
            }
            Statement statement = connection.createStatement();
            statement.execute(sql);
            statement.close();
            connection.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * save broken links
     */
    public boolean saveDeadLinkDomain(DeadLinkDomain model) {
        String dateStr = SDF.format(new Date());
        try {
            Connection connection = this.getConnection();
            if (connection == null) {
                return false;
            }
            Statement statement = connection.createStatement();
            Integer statusCode = model.getStatusCode();
            String reasonPhrase = model.getReasonPhrase();
            String domainUrl = model.getDomainUrl();
            Integer linkNumber = model.getLinkNumber();
            Integer page = model.getPage();
            String detailLink = model.getDetailLink();
            String deadLinkTitle = model.getDeadLinkTitle();
            String sql = "INSERT INTO dead_link_domain_" + dateStr + "(status_code, reason_phrase, domain_url, link_number, page, detail_link, dead_link_title) VALUE (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, statusCode);
            preparedStatement.setString(2, reasonPhrase);
            preparedStatement.setString(3, domainUrl);
            preparedStatement.setInt(4, linkNumber);
            preparedStatement.setInt(5, page);
            preparedStatement.setString(6, detailLink);
            preparedStatement.setString(7, deadLinkTitle);
            preparedStatement.execute();
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
     * select broken domain
     *
     * @return
     */
    public List<DeadLinkDomain> selectDeadLinkDomain(String dateStr) {
        List<DeadLinkDomain> list = new ArrayList<>();
        try {
            Connection connection = this.getConnection();
            if (connection == null) {
                return list;
            }
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM dead_link_domain_" + dateStr;
            ResultSet resultSet = statement.executeQuery(sql);
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
            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * select domain from black list
     *
     * @return
     */
    public List<BlackListDomain> selectBlackListDomain() {
        List<BlackListDomain> list = new ArrayList<>();
        try {
            Connection connection = this.getConnection();
            if (connection == null) {
                return list;
            }
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM black_list_domain";
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                BlackListDomain model = new BlackListDomain();
                model.setId(resultSet.getLong(1));
                model.setDomainUrl(resultSet.getString(2));
                model.setCreateTime(resultSet.getTimestamp(3));
                list.add(model);
            }
            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * select domain from the white list
     *
     * @return
     */
    public List<WhiteListDomain> selectWhiteListDomain() {
        List<WhiteListDomain> list = new ArrayList<>();
        try {
            Connection connection = this.getConnection();
            if (connection == null) {
                return list;
            }
            Statement statement = connection.createStatement();
            String sql = "SELECT * FROM white_list_domain";
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                WhiteListDomain model = new WhiteListDomain();
                model.setId(resultSet.getLong(1));
                model.setDomainUrl(resultSet.getString(2));
                model.setCreateTime(resultSet.getTimestamp(3));
                list.add(model);
            }
            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * save data of broken link
     */
    public boolean saveCollectInfo(CollectInfo model) {
        try {
            Connection connection = this.getConnection();
            if (connection == null) {
                return false;
            }
            Statement statement = connection.createStatement();
            //get the parameters
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
            //use parameter ? (space)
            String sql = "INSERT INTO collect_info(events, events_dead, materials, materials_dead, elearning, elearning_dead, workflows, workflows_dead, domain_dead, create_date) VALUE (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
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
            preparedStatement.execute();
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
     * select collected data by Date
     *
     * @return
     */
    public List<CollectInfo> selectCollectInfoByDate(List<String> dateList) {
        List<CollectInfo> list = new ArrayList<>();
        try {
            Connection connection = this.getConnection();
            if (connection == null) {
                return list;
            }
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
            ResultSet resultSet = preparedStatement.executeQuery();
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
            resultSet.close();
            preparedStatement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * delete the collected data
     *
     * @param date
     * @return
     */
    public boolean deleteCollectInfo(String date) {
        try {
            Connection connection = this.getConnection();
            if (connection == null) {
                return false;
            }
            Statement statement = connection.createStatement();
            String sql = "DELETE FROM collect_info WHERE create_date = '" + date + "'";
            statement.execute(sql);
            statement.close();
            connection.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
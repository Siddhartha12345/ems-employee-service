package com.employee.config;

import com.employee.exception.GenericException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class EmployeeIdSequenceGenerator implements IdentifierGenerator {

    private static final String prefix = "EID";

    private static final Logger LOGGER = LoggerFactory.getLogger(EmployeeIdSequenceGenerator.class);

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        String generatedId;
        try(Connection connection = session.getJdbcConnectionAccess().obtainConnection();) {
            Statement statement=connection.createStatement();
            // if empid exists in emp_garbage_tbl then fetch and return it, and delete it from th table since it is assigned now
            ResultSet garbageResultSet = statement.executeQuery("select employee_id from employeedb.emp_garbage_tbl LIMIT 1");
            if(garbageResultSet.next()) {
                generatedId = garbageResultSet.getString(1);
                int delRow = statement.executeUpdate("delete from employeedb.emp_garbage_tbl where employee_id='" + generatedId + "'");
                if(delRow > 0) {
                    LOGGER.info("Employee ID {} deleted successfully from employeedb.emp_garbage_tbl",generatedId);
                } else {
                    LOGGER.error("Unable to delete Employee ID {} from employeedb.emp_garbage_tbl", generatedId);
                    throw new GenericException("Emp ID " + generatedId + " could not be deleted from employeedb.emp_garbage_tbl");
                }
                return generatedId;
            }
            // else calculate the count from employee table, increment it by 1 and return the value as empid
            ResultSet employeeResultSet = statement.executeQuery("select count(employee_id) as Id from employeedb.employee");
            if(employeeResultSet.next()) {
                int id = employeeResultSet.getInt(1) + 1;
                generatedId = prefix + id;
                LOGGER.info("Generated Id: {}", generatedId);
                return generatedId;
            }
        } catch (SQLException e) {
            LOGGER.error("Encountered exception while generating sequence for employee ID: {}", e);
        }
        return null;
    }
}

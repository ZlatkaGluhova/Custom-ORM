package orm;

import annotations.Column;
import annotations.Entity;
import annotations.Id;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EntityManager<E> implements DBContext<E> {

    private Connection connection;

    public EntityManager(Connection connection) {
        this.connection = connection;
    }

    @Override
    public boolean persist(E entity) throws IllegalAccessException, SQLException {
        Field primaryKey = getIdColumn(entity.getClass());
        primaryKey.setAccessible(true); //primaryKey = idColumn
        Object idValue = primaryKey.get(entity);

        if (idValue == null || (long) idValue <= 0) {
            return doInsert(entity);
        }
//        return doUpdate(entity, primaryKey);
        return false;
    }

    private boolean doUpdate(E entity, Field primaryKey) throws SQLException, IllegalAccessException {
        String tableName = getTableName(entity.getClass());
        String tableFields = getColumnsWithoutId(entity.getClass());
        String tableValues = getColumnsValuesWithoutId(entity);

        String insertQuery = String.format("UPDATE %s SET %s = %s WHERE id = %s", tableName, tableFields, tableValues, primaryKey);


        /*
        UPDATE table_name
        SET column1 = value1, column2 = value2, ...
        WHERE condition;
         */
        return connection.prepareStatement(insertQuery).execute();
    }

    private boolean doInsert(E entity) throws SQLException, IllegalAccessException {
        String tableName = getTableName(entity.getClass());
        String tableFields = getColumnsWithoutId(entity.getClass());
        String tableValues = getColumnsValuesWithoutId(entity);

        String insertQuery = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, tableFields, tableValues);

        /*
        INSERT INTO table_name (column1, column2, column3, ...)
        VALUES (value1, value2, value3, ...);
         */
        return connection.prepareStatement(insertQuery).execute();
    }

    @Override
    public Iterable<E> find(Class<E> table) {
        return null;
    }

    @Override
    public Iterable<E> find(Class<E> table, String where) {
        return null;
    }

    @Override
    public E findFirst(Class<E> table) {
        return null;
    }

    @Override
    public E findFirst(Class<E> table, String where) {
        return null;
    }

    private Field getIdColumn(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Id.class))
                .findFirst()
                .orElseThrow(() ->
                        new UnsupportedOperationException("Entity does not have primary key"));
//        ДРУГ ВАРИАНТ НА СТРИЙМА:
//        Field[] declaredFields = clazz.getDeclaredFields();
//
//        for (Field declaredField : declaredFields) {
//            boolean annotationPresent = declaredField.isAnnotationPresent(Id.class);
//
//            if (annotationPresent) {
//                return declaredField;
//            }
//        }
//        throw new UnsupportedOperationException("Entity missing an Id column");
    }

    private String getTableName(Class<?> aClass) {
        Entity[] annotationsByType = aClass.getAnnotationsByType(Entity.class);
        if (annotationsByType.length == 0) {
            throw new UnsupportedOperationException("Class must be Entity");
        }
        return annotationsByType[0].name();
    }


    private String getColumnsValuesWithoutId(E entity) throws IllegalAccessException {
        Class<?> aClass = entity.getClass();
        List<Field> fields = Arrays.stream(aClass.getDeclaredFields())
                .filter(f -> !f.isAnnotationPresent(Id.class))
                .filter(f -> f.isAnnotationPresent(Column.class))
                .collect(Collectors.toList());

        List<String> values = new ArrayList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            Object o = field.get(entity);

            if (o instanceof String || o instanceof LocalDate) {
                values.add("'" + o + "'");
            } else {
                values.add(o.toString());
            }



        }

        return String.join(",", values);
    }

    private String getColumnsWithoutId(Class<?> aClass) {

        return Arrays.stream(aClass.getDeclaredFields())
                .filter(f -> !f.isAnnotationPresent(Id.class))
                .filter(f -> f.isAnnotationPresent(Column.class))
                .map(f -> f.getAnnotationsByType(Column.class))
                .map(a -> a[0].name())
                .collect(Collectors.joining(","));
    }
}

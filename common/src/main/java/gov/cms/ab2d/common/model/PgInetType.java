package gov.cms.ab2d.common.model;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.postgresql.util.PGobject;

public class PgInetType implements UserType {

    public PgInetType() {

    }

    @Override
    public Object assemble(Serializable cached, Object owner) {
        return deepCopy(cached);
    }

    @Override
    public Object deepCopy(Object value) {
        if (value != null) {
            return new PGInet(((PGInet) value).getAddress());
        }
        return null;
    }

    @Override
    public Serializable disassemble(Object value) {
        return (value != null) ? (Serializable) deepCopy(value) : null;
    }

    @Override
    public boolean equals(Object x, Object y) {
        return x == y || x != null && y != null && x.equals(y);
    }

    @Override
    public int hashCode(Object x) {
        return (x != null) ? x.hashCode() : 0;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names,
                              SharedSessionContractImplementor session, Object owner) throws SQLException {
        PGInet address = null;

        String ipStr = rs.getString(names[0]);
        if (ipStr != null) {
            try {
                address = new PGInet(InetAddress.getByName(ipStr));
            } catch (UnknownHostException e) {
                throw new HibernateException(e);
            }
        }

        return address;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index,
                            SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.VARCHAR);
        } else {
            PGobject pgObj = new PGobject();
            pgObj.setType("inet");
            pgObj.setValue(((PGInet) value).getAddress().getHostAddress());
            st.setObject(index, pgObj);
        }
    }

    @Override
    public Object replace(Object original, Object target, Object owner) {
        return deepCopy(original);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class returnedClass() {
        return PGInet.class;
    }

    @Override
    public int[] sqlTypes() {
        return new int[] {Types.OTHER};
    }

}
package gov.cms.ab2d.common.model;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

@SuppressWarnings("serial")
public class PGInet implements Serializable {

    private InetAddress address;

    public PGInet() {

    }

    public PGInet(InetAddress address) {
        this.address = address;
    }

    public PGInet(String address) throws UnknownHostException {
        this(InetAddress.getByName(address));
    }

    public InetAddress getAddress() {
        return address;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof PGInet)) {
            return false;
        }
        PGInet other = (PGInet) obj;
        if (address == null) {
            return other.address == null;
        } else if (!address.equals(other.address)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PgInet [address=");
        builder.append(address);
        builder.append("]");
        return builder.toString();
    }

}

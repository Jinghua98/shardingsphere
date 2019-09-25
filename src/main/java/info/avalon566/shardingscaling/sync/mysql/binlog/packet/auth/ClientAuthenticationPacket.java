package info.avalon566.shardingscaling.sync.mysql.binlog.packet.auth;

import com.google.common.base.Strings;
import info.avalon566.shardingscaling.sync.mysql.binlog.codec.Capability;
import info.avalon566.shardingscaling.sync.mysql.binlog.MySQLPasswordEncryptor;
import info.avalon566.shardingscaling.sync.mysql.binlog.codec.DataTypesCodec;
import info.avalon566.shardingscaling.sync.mysql.binlog.packet.AbstractPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.Data;
import lombok.var;

import java.security.NoSuchAlgorithmException;

/**
 * @author avalon566
 */
@Data
public class ClientAuthenticationPacket extends AbstractPacket {
    private int clientCapability = Capability.CLIENT_LONG_PASSWORD | Capability.CLIENT_LONG_FLAG
            | Capability.CLIENT_PROTOCOL_41 | Capability.CLIENT_INTERACTIVE
            | Capability.CLIENT_TRANSACTIONS | Capability.CLIENT_SECURE_CONNECTION
            | Capability.CLIENT_MULTI_STATEMENTS | Capability.CLIENT_PLUGIN_AUTH;
    private String username;
    private String password;
    private byte charsetNumber;
    private String databaseName;
    private int serverCapabilities;
    private byte[] scrumbleBuff;
    private String authPluginName;

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
        if (databaseName != null) {
            this.clientCapability |= Capability.CLIENT_CONNECT_WITH_DB;
        }
    }

    public void setAuthPluginName(String authPluginName) {
        this.authPluginName = authPluginName;
        if (authPluginName != null) {
            this.clientCapability |= Capability.CLIENT_PLUGIN_AUTH;
        }
    }

    @Override
    public ByteBuf toByteBuf() {
        var out = ByteBufAllocator.DEFAULT.heapBuffer();
        DataTypesCodec.writeInt(clientCapability, out);
        DataTypesCodec.writeInt(1 << 24, out);
        DataTypesCodec.writeByte(charsetNumber, out);
        DataTypesCodec.writeBytes(new byte[23], out);
        DataTypesCodec.writeBytes(getUsername().getBytes(), out);
        DataTypesCodec.writeByte((byte) 0x00, out);
        if (Strings.isNullOrEmpty(getPassword())) {
            DataTypesCodec.writeByte((byte) 0x00, out);
        } else {
            try {
                byte[] encryptedPassword = MySQLPasswordEncryptor.scramble411(getPassword().getBytes(), scrumbleBuff);
                DataTypesCodec.writeLengthCodedBinary(encryptedPassword, out);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("can't encrypt password that will be sent to MySQL server.", e);
            }
        }
        if (getDatabaseName() != null) {
            DataTypesCodec.writeNullTerminatedString(getDatabaseName(), out);
        }
        if (getAuthPluginName() != null) {
            DataTypesCodec.writeNullTerminatedString(getAuthPluginName(), out);
        }
        return out;
    }
}

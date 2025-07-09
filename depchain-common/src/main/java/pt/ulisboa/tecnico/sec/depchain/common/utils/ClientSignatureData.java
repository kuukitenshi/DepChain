package pt.ulisboa.tecnico.sec.depchain.common.utils;

import java.io.Serializable;

import org.apache.tuweni.bytes.Bytes;
import org.web3j.crypto.Sign.SignatureData;

public record ClientSignatureData(String v, String r, String s) implements Serializable {

    public ClientSignatureData(SignatureData data) {
        this(Bytes.wrap(data.getV()).toUnprefixedHexString(),
                Bytes.wrap(data.getR()).toUnprefixedHexString(),
                Bytes.wrap(data.getS()).toUnprefixedHexString());
    }

    public SignatureData toSignatureData() {
        byte[] v = Bytes.fromHexString(this.v).toArray();
        byte[] r = Bytes.fromHexString(this.r).toArray();
        byte[] s = Bytes.fromHexString(this.s).toArray();
        return new SignatureData(v, r, s);
    }

}

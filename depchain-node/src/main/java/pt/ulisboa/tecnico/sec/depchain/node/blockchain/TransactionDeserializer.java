package pt.ulisboa.tecnico.sec.depchain.node.blockchain;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import pt.ulisboa.tecnico.sec.depchain.common.protocol.ContractTransaction;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.ExternalTransaction;
import pt.ulisboa.tecnico.sec.depchain.common.protocol.Transaction;

public class TransactionDeserializer implements JsonDeserializer<Transaction> {

    @Override
    public Transaction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        Class<?> clazz = ExternalTransaction.class;
        if (jsonObject.has("callData")) {
            clazz = ContractTransaction.class;
        }
        return context.deserialize(json, clazz);
    }

}

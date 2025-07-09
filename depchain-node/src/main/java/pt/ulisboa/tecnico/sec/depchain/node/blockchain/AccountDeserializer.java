package pt.ulisboa.tecnico.sec.depchain.node.blockchain;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class AccountDeserializer implements JsonDeserializer<Account> {

    @Override
    public Account deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        Class<?> clazz = ExternalAccount.class;
        if (jsonObject.has("code")) {
            clazz = ContractAccount.class;
        }
        return context.deserialize(json, clazz);
    }

}

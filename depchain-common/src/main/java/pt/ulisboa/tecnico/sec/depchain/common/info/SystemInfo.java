package pt.ulisboa.tecnico.sec.depchain.common.info;

import java.io.File;
import java.util.List;

import com.moandjiezana.toml.Toml;

public record SystemInfo(List<NodeInfo> nodes) {

    public static SystemInfo load(File membersFile) {
        Toml toml = new Toml().read(membersFile);
        Toml nodes = toml.getTable("nodes");
        List<NodeInfo> infos = nodes.entrySet().stream().map(entry -> {
            String id = entry.getKey();
            Toml node = (Toml) entry.getValue();
            String hostname = node.getString("hostname");
            int nodePort = node.getLong("node_port").intValue();
            int clientPort = node.getLong("client_port").intValue();
            NodeInfo info = new NodeInfo(id, hostname, nodePort, clientPort);
            return info;
        }).toList();
        return new SystemInfo(infos);
    }
}

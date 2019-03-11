package pw.riku.notileentity;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;

/**
 * 設定を管理するためのクラスです。
 */
public class Configuration {
    /**
     * 読み込み対象
     */
    private final Path filePath;

    /**
     * 対象のエンティティリスト
     */
    private Set<Material> targetBlocks;

    public Configuration(Path filePath) throws IOException, InvalidConfigurationException {
        this.filePath = filePath;
        load();
    }

    /**
     * 設定を設定ファイルから読み込みます。
     *
     * @throws IOException                   入出力エラーが発生した場合
     * @throws InvalidConfigurationException 無効な設定ファイルだった場合
     */
    public void load() throws IOException, InvalidConfigurationException {
        org.bukkit.configuration.Configuration configuration = loadConfiguration();

        loadTargetBlocks(configuration);
    }

    /**
     * 対象のブロックリストを読み込みます。
     *
     * @param configuration Configuration
     * @throws InvalidConfigurationException 無効な設定ファイルだった場合
     */
    private void loadTargetBlocks(org.bukkit.configuration.Configuration configuration)
        throws InvalidConfigurationException {

        targetBlocks = EnumSet.noneOf(Material.class);

        for (String blocks : configuration.getStringList("blocks")) {
            Material material;
            try {
                material = Material.valueOf(blocks);
            } catch (IllegalArgumentException e) {
                throw new InvalidConfigurationException("'" + blocks + "' is invalid material name!");
            }
            targetBlocks.add(material);
        }
    }

    /**
     * filePathを読み込みます。
     *
     * @return ロード済みのFileConfiguration
     * @throws IOException                   入出力エラーが発生した場合
     * @throws InvalidConfigurationException 無効な設定ファイルだった場合
     */
    private org.bukkit.configuration.Configuration loadConfiguration()
        throws IOException, InvalidConfigurationException {

        YamlConfiguration configuration = new YamlConfiguration();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            configuration.load(reader);
        }
        return configuration;
    }

    /**
     * このクラスの読み込み対象となっている設定ファイルを返します。
     *
     * @return Path
     */
    public Path getFilePath() {
        return filePath;
    }

    /**
     * 対象のブロックリストを返します。
     *
     * @return ブロックリスト
     */
    public Set<Material> getTargetBlocks() {
        return targetBlocks;
    }
}

package deponn.depmount;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * コマンドを処理するパーサークラス
 */
public class CommandParser {
    /**
     * パース成功したか
     */
    public final boolean isSuccess;
    /**
     * 全置き換えモード、trueの場合空気ブロック以外も置き換える
     */
    public final boolean bReplaceAll;
    /**
     * 境界にラピスラズリブロック配置モード、trueの場合境界をなめらかにする
     */
    public final boolean bCollectBorder;
    /**
     * 補間する頂点(ラピスラズリブロック)の数
     */
    public final int numInterpolationPoints;

    // パース成功
    private CommandParser(boolean isSuccess, boolean bReplaceAll, boolean bCollectBorder, int numInterpolationPoints) {
        this.isSuccess = isSuccess;
        this.bReplaceAll = bReplaceAll;
        this.bCollectBorder = bCollectBorder;
        this.numInterpolationPoints = numInterpolationPoints;
    }

    // パース失敗
    private CommandParser() {
        this(false, false, false, 0);
    }

    /**
     * コマンドのTAB補完候補を返す
     *
     * @param sender コマンド送信者
     * @param args   引数
     * @return コマンド補完候補
     */
    public static List<String> suggestCommand(CommandSender sender, String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.size() > 1 && "-n".equals(argsList.get(argsList.size() - 2))) {
            return Arrays.asList("0", "5", "20");
        } else {
            return Stream.of("-a", "-b", "-n")
                    .filter(s -> !argsList.contains(s))
                    .collect(Collectors.toList());
        }
    }

    /**
     * コマンドをパースする
     *
     * @param sender コマンド送信者
     * @param args   引数
     * @return コマンド補完候補
     */
    public static CommandParser parseCommand(CommandSender sender, String[] args) {
        List<String> argsList = Arrays.asList(args);
        boolean bReplaceAll = false;
        boolean bCollectBorder = false;
        int numInterpolationPoints = 0;
        if (argsList.contains("-a")) {
            // 全置き換えモード、trueの場合空気ブロック以外も置き換える
            bReplaceAll = true;
        }
        if (argsList.contains("-b")) {
            // 境界にラピスラズリブロック配置モード、trueの場合境界をなめらかにする
            bCollectBorder = true;
        }
        if (argsList.contains("-n")) {
            // 引数が何番目か取得し、若い番号を採用する
            int index = argsList.indexOf("-n");
            if (index + 1 >= argsList.size()) {
                // 引数の次がなかった場合、エラー
                sender.sendMessage(ChatColor.RED + "数値が必要です。 -n <数字>");
                return new CommandParser();
            }
            try {
                // 補間する頂点(ラピスラズリブロック)の数
                numInterpolationPoints = Integer.parseInt(argsList.get(index + 1));
                if (numInterpolationPoints < 0) {
                    sender.sendMessage(ChatColor.RED + "数値は正の数である必要があります。 -n <数字>");
                    return new CommandParser();
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "数値が不正です。 -n <数字>");
                return new CommandParser();
            }
        }
        return new CommandParser(true, bReplaceAll, bCollectBorder, numInterpolationPoints);
    }
}

package legacyconnector.v1_12_2.net.minecraft.util;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiTextField;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.util.text.TextFormatting;

/**
 * 1.12.2's Tab completion of a text field (the chat, command blocks): the first Tab asks for completions of the text before the cursor,
 * later presses cycle through them. Completions come from the modern client's command suggestions (commands) or the online players'
 * names (chat), delivered through {@link #setCompletions} as the server's answer was.
 */
public abstract class TabCompleter {
    protected final GuiTextField textField;
    protected final boolean hasTargetBlock;
    protected boolean didComplete;
    protected boolean requestedCompletions;
    protected int completionIdx;
    protected List<String> completions = new ArrayList<>();

    public TabCompleter(GuiTextField textFieldIn, boolean hasTargetBlockIn) {
        this.textField = textFieldIn;
        this.hasTargetBlock = hasTargetBlockIn;
    }

    public void complete() {
        if (didComplete) {
            textField.deleteFromCursor(0);
            textField.deleteFromCursor(textField.getNthWordFromPosWS(-1, textField.getCursorPosition(), false) - textField.getCursorPosition());
            if (completionIdx >= completions.size()) completionIdx = 0;
        } else {
            int i = textField.getNthWordFromPosWS(-1, textField.getCursorPosition(), false);
            completions.clear();
            completionIdx = 0;
            String s = textField.getText().substring(0, textField.getCursorPosition());
            requestCompletions(s);
            if (completions.isEmpty()) return;
            didComplete = true;
            textField.deleteFromCursor(i - textField.getCursorPosition());
        }
        textField.writeText(TextFormatting.getTextWithoutFormattingCodes(completions.get(completionIdx++)));
    }

    /** Asks for completions of {@code prefix}; they arrive later through {@link #setCompletions} on the client thread. */
    private void requestCompletions(String prefix) {
        if (prefix.isEmpty()) return;
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return;
        requestedCompletions = true;
        var connection = mc.player.connection;
        if (prefix.startsWith("/")) {
            String command = prefix.substring(1);
            var parse = connection.getCommands().parse(command, connection.getSuggestionsProvider());
            connection.getCommands().getCompletionSuggestions(parse).thenAccept(suggestions -> {
                List<String> out = new ArrayList<>();
                int start = suggestions.getRange().getStart();
                for (var s : suggestions.getList()) {
                    String full = command.substring(0, Math.min(start, command.length())) + s.getText();
                    int lastSpace = full.lastIndexOf(' ');
                    out.add(lastSpace < 0 ? "/" + full : full.substring(lastSpace + 1));
                }
                mc.tell(() -> setCompletions(out.toArray(new String[0])));
            });
        } else {
            int space = prefix.lastIndexOf(' ');
            String word = (space < 0 ? prefix : prefix.substring(space + 1)).toLowerCase(java.util.Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (var info : connection.getOnlinePlayers()) {
                String name = info.getProfile().getName();
                if (name.toLowerCase(java.util.Locale.ROOT).startsWith(word)) out.add(name);
            }
            mc.tell(() -> setCompletions(out.toArray(new String[0])));
        }
    }

    public abstract BlockPos getTargetBlockPos();

    public void setCompletions(String... newCompl) {
        if (!requestedCompletions) return;
        didComplete = false;
        completions.clear();
        for (String s : newCompl) if (s != null && !s.isEmpty()) completions.add(s);
        String s1 = textField.getText().substring(textField.getNthWordFromPosWS(-1, textField.getCursorPosition(), false));
        String s2 = TextFormatting.getTextWithoutFormattingCodes(org.apache.commons.lang3.StringUtils.getCommonPrefix(newCompl));
        if (s2 != null && !s2.isEmpty() && !s1.equalsIgnoreCase(s2)) {
            textField.deleteFromCursor(0);
            textField.deleteFromCursor(textField.getNthWordFromPosWS(-1, textField.getCursorPosition(), false) - textField.getCursorPosition());
            textField.writeText(s2);
        } else if (!completions.isEmpty()) {
            didComplete = true;
            complete();
        }
    }

    public void resetDidComplete() {
        didComplete = false;
    }

    public void resetRequested() {
        requestedCompletions = false;
    }
}

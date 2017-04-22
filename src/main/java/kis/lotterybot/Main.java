package kis.lotterybot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.linecorp.bot.client.LineMessagingService;
import com.linecorp.bot.model.ReplyMessage;
import com.linecorp.bot.model.event.Event;
import com.linecorp.bot.model.event.MessageEvent;
import com.linecorp.bot.model.event.message.TextMessageContent;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.StickerMessage;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.spring.boot.annotation.EventMapping;
import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@LineMessageHandler
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    private static final int[][] STICKERS = {
        {2, 166},
        {2, 144},
        {2, 36},
        {1, 138},
        {1, 114},
    };

    private static final Path ROOT_PATH = Paths.get("./data");
    private static final Path SHUFFLE_PATH = ROOT_PATH.resolve("shuffle.txt");
    private static final String MEMBER_DEFAULT_FILE = "/member.txt";
    static final Path MEMBER_PATH = ROOT_PATH.resolve("member.txt");

    static class CounterFile {
        Path filePath;

        CounterFile(String filename) {
            filePath = ROOT_PATH.resolve(filename);
        }

        int getValue() {
            try (BufferedReader bur = Files.newBufferedReader(filePath)) {
                return bur.lines().mapToInt(Integer::parseInt).findFirst().orElse(0);
            }catch (IOException | NumberFormatException ex) {
                // do nothing
            }
            return 0;
        }
        void setValue(int value) {
            try {
                Path parent = filePath.getParent();
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.write(filePath, Collections.singletonList(value + ""));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        void reset() {
            setValue(0);
        }
    }

    private final CounterFile indexFile = new CounterFile("idx.txt");
    private final CounterFile skipFile = new CounterFile("skip.txt");

    @Autowired
    private LineMessagingService lineMessagingService;

    @EventMapping
    public void handleTextMessageEvent(MessageEvent<TextMessageContent> event) {
        String replyToken = event.getReplyToken();
        try {
            String command = event.getMessage().getText();
            if ("help".equalsIgnoreCase(command)) {
                sendText(replyToken,
                        "使い方\n"
                        + "開始(start):最初から\n"
                        + "抽選(sel):抽選します\n"
                        + "人数(count):参加人数\n"
                );
            } else if ("抽選".equals(command) || "sel".equalsIgnoreCase(command)) {
                    if (!Files.exists(ROOT_PATH)) {
                        Files.createDirectories(ROOT_PATH);
                    }
                    if (!Files.exists(SHUFFLE_PATH)) {
                        start();
                    }
                    int idx = indexFile.getValue();
                    if (idx >= getMemberCount()) {
                        sendText(replyToken, "抽選終了です");
                    } else {
                        try (BufferedReader bur = Files.newBufferedReader(SHUFFLE_PATH)) {
                            String names = bur.lines().skip(idx).findFirst().get();
                            String[] name = names.split(",");
                            int[] sticker = STICKERS[idx % STICKERS.length];
                            sendTextAndSticker(replyToken,
                                    String.format("%sさんと%sさんがペアです！", name[0], name[1]), 
                                    sticker[0], sticker[1]);
                        }
                        indexFile.setValue((idx + 1) % getMemberCount());
                    }
            } else if ("開始".equals(command) || "start".equalsIgnoreCase(command)) {
                start();
                sendTextAndSticker(replyToken, "抽選開始!!", 2, 45);
            } else if ("人数".equals(command) || "count".equalsIgnoreCase(command)) {
                sendText(replyToken, String.format("%d人参加です！", getMemberCount()));
            } else {
                // do nothing
            }
        } catch (Exception ex) {
            sendText(replyToken, ex.getMessage());
        }
    }

    private void start() throws IOException{
        if (!Files.exists(ROOT_PATH)) {
            Files.createDirectories(ROOT_PATH);
        }
        try (InputStream is = getMemberFileInput();
             BufferedReader bur = new BufferedReader(new InputStreamReader(is))) {
            List<String> lines = bur.lines()
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.replaceAll("\t", " ").replaceAll("　", " "))
                    .collect(Collectors.toList());
            
            List<List<String>> members = new ArrayList();
            List<String> list = new ArrayList<>();
            for (String m : lines) {
                if (m.startsWith("--")) {
                    members.add(list);
                    list = new ArrayList<>();
                } else {
                    list.add(m);
                }
            }
            members.add(list);
            
            members.forEach(Collections::shuffle);
            List<String> pairs = new ArrayList<>();
            for (List<String> member : members) {
                for (int i = 0; i < member.size(); i += 2) {
                    pairs.add(String.format("%s,%s", member.get(i), member.get(i + 1)));
                }
            }
            Files.write(SHUFFLE_PATH, pairs);

            indexFile.reset();
            skipFile.reset();
        }
    }
    private int getMemberCount() throws IOException {
        try (InputStream is = getMemberFileInput();
             BufferedReader bur = new BufferedReader(new InputStreamReader(is))) {
            return (int) bur.lines().filter(s -> !s.isEmpty()).count();
        }
    }
    private InputStream getMemberFileInput() throws IOException {
        if (Files.exists(MEMBER_PATH)) {
            return Files.newInputStream(MEMBER_PATH);
        }else {
            return Main.class.getResourceAsStream(MEMBER_DEFAULT_FILE);
        }
    }

    @EventMapping
    public void handleDefaultMessageEvent(Event event) {
        System.out.println("event:" + event);
    }

    private void sendText(String replyToken, String message) {
        sendMessages(replyToken, new TextMessage(message));
    }

    private void sendTextAndSticker(String replyToken, String message, int packageId, int sticdkerId) {
        sendMessages(replyToken, new TextMessage(message), new StickerMessage("" + packageId, "" + sticdkerId));
    }

    private void sendMessages(String replyToken, Message... messages) {
        try {
            lineMessagingService.replyMessage(new ReplyMessage(replyToken, Arrays.asList(messages)))
                    .execute();
        }catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

    }

}

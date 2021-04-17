package com.example.plugin;

import com.example.util.CommonDefinition;
import com.example.util.FileUtil;
import com.example.util.PluginSystemBase;
import net.mamoe.mirai.console.extension.PluginComponentStorage;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SaucenaoPlugin extends PluginSystemBase {
    public static SaucenaoPlugin INSTANCE = new SaucenaoPlugin();
    public static int shortLimit = -1, longLimit = -1;

    private SaucenaoPlugin() {
        super(new JvmPluginDescriptionBuilder(
                        "com.example.plugin.SaucenaoPlugin", // 需要遵循语法规定，不知道写什么的话就写主类名吧
                        "1.0.1" // 同样需要遵循语法规定
                )
                        .author("Horizon")
                        .name("SaucenaoPlugin")
                        .info("SauceNAO搜图插件")
                        .build()
        );
    }

    @Override
    public void onLoad(@NotNull PluginComponentStorage $this$onLoad) {
        File statFile = new File(CommonDefinition.fileRootPath + CommonDefinition.statFileName);
        if (!statFile.exists()) {
            try {
                statFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onEnable() {
        GlobalEventChannel.INSTANCE.subscribeAlways(MessageEvent.class, (MessageEvent event) -> {

            //TODO: Quota support for all users
            //TODO: Blacklist

            String content = event.getMessage().contentToString();
            long sender = event.getSender().getId();
            System.out.println(content);

            if (content.contains("出处")) {
                File statFile = new File(CommonDefinition.fileRootPath + CommonDefinition.statFileName);
                try {
                    BufferedWriter statFileWriter = new BufferedWriter(new FileWriter(statFile));
                    statFileWriter.append(String.valueOf(new Date().getTime())).append(",").append(String.valueOf(sender));
                    statFileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ResponseInfo responseInfo = super.request(sender);

                if (responseInfo.rejectReason == RejectReason.LongLimitReached) {
                    MessageChain messages = new At(sender).plus(" 你已经达到长上限啦, 稍等会儿再来\n长上限每小时刷新一次~");
                    event.getSubject().sendMessage(messages);
                    return;
                }
                if (responseInfo.rejectReason == RejectReason.ShortLimitReached) {
                    MessageChain messages = new At(sender).plus(" 你已经达到短上限啦, 稍等会儿再来\n短上限每40秒刷新一次~");
                    event.getSubject().sendMessage(messages);
                    return;
                }

                MessageChain chain = event.getMessage();
                Image image = chain.get(Image.Key);
                String imageName = CommonDefinition.fileRootPath + FileUtil.getTimeStamp() + ".jpg";
                if (image != null) {
                    try {
                        FileUtil.download(Image.queryUrl(image), imageName);
                        event.getSubject().sendMessage(new At(sender).
                                plus(" 收到啦\n你的短上限还剩" + responseInfo.remainShort
                                        + "\n你的长上限还剩" + responseInfo.remainLong));
                    } catch (Exception e) {
                        System.out.println("[Error] Failed to download " + imageName);
                        event.getSubject().sendMessage("校园网又又又又出错了, 图没下下来...要不重发一下?");
                        e.printStackTrace();
                    }
                }

                //DBG
                System.out.println("[New Req] " + sender + " " + imageName);

                String imagePath = new File(imageName).getAbsolutePath();
                Process pythonProc;
                List<String> output = new ArrayList<>();
                int returnVal = -1;
                try {
                    pythonProc = Runtime.getRuntime().exec("python " + CommonDefinition.fileRootPath + "saucenao.py " + imagePath);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(pythonProc.getInputStream()));
                    pythonProc.waitFor();
                    String buffer = bufferedReader.readLine();
                    while (buffer != null) {
                        output.add(buffer);

                        //DBG
                        System.out.println(buffer);

                        buffer = bufferedReader.readLine();
                    }
                    returnVal = pythonProc.exitValue();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

                if (returnVal == 403) {
                    event.getSubject().sendMessage("⚠ 出了点儿小问题, 喵酱的 API Key 被服务器拒绝了, 快去找 Horizon");
                    return;
                }
                if (returnVal != 200 && returnVal != 0) {
                    System.out.println("[Error] Unhandled returnVal of Python: " + returnVal);
                    event.getSubject().sendMessage("图片没 Post 到服务器, 要喵酱说这锅得归校园网背🔨");
                    return;
                }

                shortLimit = Integer.parseInt(output.get(0));
                longLimit = Integer.parseInt(output.get(1));
                double similarity = Double.parseDouble(output.get(2));
                String database = output.get(3), memberName = null,
                        title = null, fileName = null, url = null, pid = null;

                if (similarity < CommonDefinition.minSimilarity) {
                    MessageChain messages = new PlainText("喵酱已经努力找了, 不过没有找到足够相似的")
                            .plus(new Face(Face.KUAI_KU_LE).plus(new At(sender)));
                    event.getSubject().sendMessage(messages);
                    return;
                }

                if (database.equals("Pixiv")) {
                    memberName = output.get(4);
                    title = output.get(5);
                    fileName = output.get(6);
                    url = output.get(7);
                    pid = output.get(8);
                } else {
                    url = output.get(4);
                }

                StringBuilder response = new StringBuilder();
                response.append("在 ").append(database).append(" 中找到啦!\n");
                response.append("相似度: ").append(similarity).append("%\n");
                if (database.equals("Pixiv")) {
                    response.append("画师: ").append(memberName).append("\n");
                    response.append("标题: ").append(title).append("\n");
                    response.append("pid: ").append(pid);
                }
                event.getSubject().sendMessage(response.toString());
                // TODO
                // response.append("link: ").append(url);

                Image upload = ExternalResource.uploadAsImage(
                        new File(CommonDefinition.fileRootPath + fileName), event.getSubject());
                MessageChain messages = new PlainText("").plus(upload);
                event.getSubject().sendMessage(messages);
            }
        });
    }
}

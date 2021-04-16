package com.example.plugin;

import com.example.util.CommonDefinition;
import com.example.util.FileUtil;
import net.mamoe.mirai.console.extension.PluginComponentStorage;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SaucenaoPlugin extends JavaPlugin {
    public static SaucenaoPlugin INSTANCE = new SaucenaoPlugin();
    public static int shortLimit = -1, longLimit = -1;

    private SaucenaoPlugin() {
        super(new JvmPluginDescriptionBuilder(
                        "com.example.plugin.SaucenaoPlugin", // 需要遵循语法规定，不知道写什么的话就写主类名吧
                        "0.0.1" // 同样需要遵循语法规定
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
        GlobalEventChannel.INSTANCE.subscribeAlways(GroupMessageEvent.class, (GroupMessageEvent event) -> {
            String content = event.getMessage().contentToString();
            long sender = event.getSender().getId();
            System.out.println(content);

            if (sender == 1783861062L && content.contains("出处")) {
                File statFile = new File(CommonDefinition.statFileName);
                try {
                    BufferedWriter statFileWriter = new BufferedWriter(new FileWriter(statFile));
                    //IMPROVE
                    //TimeStamp should not contain "_"
                    statFileWriter.append(FileUtil.getTimeStamp() + "," + sender);
                    statFileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                event.getSubject().sendMessage("在找了在找了, 稍等");
                //TEST
                System.out.println("在找了");

                MessageChain chain = event.getMessage();
                Image image = chain.get(Image.Key);
                String imageName = CommonDefinition.fileRootPath + FileUtil.getTimeStamp() + ".jpg";
                if (image != null) {
                    try {
                        FileUtil.download(Image.queryUrl(image), imageName);
                    } catch (Exception e) {
                        System.out.println(imageName);
                        e.printStackTrace();
                    }
                }

                //TEST
                System.out.println("[New Req] " + sender + " " + imageName);

                String imagePath = new File(imageName).getAbsolutePath();
                Process pythonProc;
                List<String> output = new ArrayList<>();
                int returnVal = -1;
                try {
                    pythonProc = Runtime.getRuntime().exec("python " + CommonDefinition.fileRootPath + "saucenao.py " + imagePath);
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(pythonProc.getInputStream()));
                    pythonProc.waitFor();
                    String res = bufferedReader.readLine();
                    while (res != null) {
                        output.add(res);

                        //TEST
                        System.out.println(res);

                        res = bufferedReader.readLine();
                    }
                    returnVal = pythonProc.exitValue();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

                if (returnVal == 403) {
                    event.getGroup().sendMessage("⚠ 出了点儿小问题, 喵酱的 API Key 被服务器拒绝了, 快去找 Horizon");

                    //TEST
                    System.out.println("⚠ 出了点儿小问题, 喵酱的 API Key 被服务器拒绝了, 快去找 Horizon");
                    return;
                }
                if (returnVal != 200 && returnVal != 0) {
                    event.getGroup().sendMessage("图片没 Post 到服务器, 要喵酱说这锅得归校园网背🔨");

                    //TEST
                    System.out.println("图片没 Post 到服务器, 要喵酱说这锅得归校园网背🔨");
                    return;
                }

                shortLimit = Integer.parseInt(output.get(0));
                longLimit = Integer.parseInt(output.get(1));
                double similarity = Double.parseDouble(output.get(2));
                String database = output.get(3), memberName = null, title = null, fileName = null, url = null;
                if (database.equals("Pixiv")) {
                    memberName = output.get(4);
                    title = output.get(5);
                    fileName = output.get(6);
                    url = output.get(7);
                } else {
                    url = output.get(4);
                }

                if (similarity < CommonDefinition.minSimilarity) {
                    MessageChain messages = new PlainText("喵酱已经努力找了, 不过没有找到足够相似的")
                            .plus(new Face(Face.KUAI_KU_LE).plus(new At(sender)));
                    event.getGroup().sendMessage(messages);
                    //TEST
                    System.out.println(messages.contentToString());

                    return;
                }

                StringBuilder response = new StringBuilder();
                response.append("在 ").append(database).append(" 中找到啦!\n");
                response.append("相似度: ").append(similarity).append("%\n");
                if (database.equals("Pixiv")) {
                    response.append("画师: ").append(memberName).append("\n");
                    response.append("题目: ").append(title).append("\n");
                    //TODO
                    //pid
                    //response.append("pid: ").append();
                }
                // response.append("link: ").append(url);
                Image upload = ExternalResource.uploadAsImage(
                        new File(CommonDefinition.fileRootPath + fileName), event.getSubject());

                event.getGroup().sendMessage(response.toString());
                MessageChain messages = new PlainText("").plus(upload);
                event.getGroup().sendMessage(messages);
                //TEST
                System.out.println(messages.contentToString());
            }
        });
    }
}

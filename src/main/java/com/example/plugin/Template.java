package com.example.plugin;

import net.mamoe.mirai.console.extension.PluginComponentStorage;
import net.mamoe.mirai.console.plugin.jvm.JavaPlugin;
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescriptionBuilder;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import org.jetbrains.annotations.NotNull;

public class Template extends JavaPlugin {
    public static Template INSTANCE = new Template();
    private Template() {
        super(new JvmPluginDescriptionBuilder(
                        "com.example.plugin.Test", // 需要遵循语法规定，不知道写什么的话就写主类名吧
                        "1.0.0" // 同样需要遵循语法规定
                )
                .author("Horizon")
                .name("AutoReply")
                .info("自动回复")
                .build()
        );
    }

    @Override
    public void onLoad(@NotNull PluginComponentStorage $this$onLoad) {

    }

    @Override
    public void onEnable() {
        GlobalEventChannel.INSTANCE.subscribeAlways(GroupMessageEvent.class, (GroupMessageEvent event) ->{
            String content = event.getMessage().contentToString();
            if (event.getGroup().getId() == 921662978 && event.getMessage().contentToString().contains("[mirai:at:878523110]")) {
                long sender = event.getSender().getId();
                event.getGroup().sendMessage("[mirai:at:" + sender + "] I can hear you");
            }
        });
    }
}
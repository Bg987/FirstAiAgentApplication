package com.example.FirstAiAgent.Controller;

import com.example.FirstAiAgent.Entity.MasterclassPost;
import com.example.FirstAiAgent.Repository.PostRepository;
import com.example.FirstAiAgent.Service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InstagramAgentBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final String codewordHash;
    private final AiService aiService;
    private final CodeImageService codeImageService;
    private final CloudinaryService cloudinaryService;
    private final InstagramPublisherService instagramPublisherService;
    private final PostRepository postRepository;

    private final Map<Long, Boolean> authenticatedUsers = new HashMap<>();
    private final Map<Long, PendingPost> pendingPosts = new HashMap<>();

    public InstagramAgentBot(@Value("${telegram.bot.username}") String username,
                             @Value("${telegram.bot.token}") String token,
                             @Value("${app.codeword.hash}") String hash,
                             AiService aiService,
                             CodeImageService codeImageService,
                             CloudinaryService cloudinaryService,
                             InstagramPublisherService instagramPublisherService,
                             PostRepository postRepository) {
        super(token);
        this.botUsername = username;
        this.codewordHash = hash;
        this.aiService = aiService;
        this.codeImageService = codeImageService;
        this.cloudinaryService = cloudinaryService;
        this.instagramPublisherService = instagramPublisherService;
        this.postRepository = postRepository;
    }

    @Override
    public String getBotUsername() { return botUsername; }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update);
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();

        if (text.equalsIgnoreCase("/start")) {
            authenticatedUsers.put(chatId, false);
            sendText(chatId, "🔓 *Java Masterclass Agent.* Enter security codeword:");
            return;
        }

        if (!authenticatedUsers.getOrDefault(chatId, false)) {
            if (BCrypt.checkpw(text, codewordHash)) {
                authenticatedUsers.put(chatId, true);
                sendText(chatId, "✅ *Access Granted!* Send a Java topic:");
            } else {
                sendText(chatId, "❌ *Unauthorized.*");
            }
            return;
        }

        generatePreview(chatId, text);
    }

    private void generatePreview(long chatId, String topic) {
        sendText(chatId, "🤖 Groq is architecting *" + topic + "*...");
        try {
            String rawResponse = aiService.getJavaSuggestions(topic);

            // 1. Precise splitting using our markers
            String codeSnippet = rawResponse.split("INSTAGRAM_START:")[0]
                    .replace("CODE_START:", "").trim();

            String instagramCaption = rawResponse.split("INSTAGRAM_START:")[1]
                    .split("BLOG_START:")[0].trim();

            String vastInformation = rawResponse.split("BLOG_START:")[1].trim();

            // 2. Generate Image Bytes
            byte[] imageBytes = codeImageService.generateCodeImage(codeSnippet);

            if (imageBytes != null) {
                // Store in memory
                pendingPosts.put(chatId, new PendingPost(imageBytes, instagramCaption, vastInformation, topic));

                // 3. Send Image Preview to Telegram
                InputFile inputFile = new InputFile(new ByteArrayInputStream(imageBytes), "preview.png");
                SendPhoto photo = new SendPhoto(String.valueOf(chatId), inputFile);
                photo.setCaption("👀 *INSTAGRAM PREVIEW:* " + topic);
                execute(photo);

                // 4. Send Caption & Approval Buttons
                askForApproval(chatId, instagramCaption);
            }
        } catch (Exception e) {
            sendText(chatId, "⚠️ Generation Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void askForApproval(long chatId, String description) {
        SendMessage sm = new SendMessage();
        sm.setChatId(String.valueOf(chatId));
        sm.setText("📖 *CAPTION PROPOSAL:*\n\n" + description + "\n\n🚀 *Publish to Instagram & Library?*");
        sm.setParseMode("Markdown");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton yes = new InlineKeyboardButton("✅ Yes, Post It");
        yes.setCallbackData("post_confirm");
        InlineKeyboardButton no = new InlineKeyboardButton("❌ No, Discard");
        no.setCallbackData("post_discard");

        row.add(yes); row.add(no);
        rows.add(row);
        markup.setKeyboard(rows);
        sm.setReplyMarkup(markup);

        try { execute(sm); } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleCallback(Update update) {
        String data = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        if (data.equals("post_discard")) {
            pendingPosts.remove(chatId);
            updateStatus(chatId, messageId, "🗑️ *Post discarded.*");
            return;
        }

        if (data.equals("post_confirm")) {
            PendingPost post = pendingPosts.get(chatId);
            if (post == null) {
                updateStatus(chatId, messageId, "⚠️ Session expired.");
                return;
            }

            updateStatus(chatId, messageId, "⏳ *Publishing & Saving to Database...*");

            try {
                // 1. Cloudinary Upload
                String publicUrl = cloudinaryService.uploadImage(post.imageBytes);
                String InstagramUrl = makeInstagramReady(publicUrl); // Transform to Square

                if (publicUrl != null) {
                    // 2. Instagram Publish
                    String igPostId = instagramPublisherService.publishToInstagram(InstagramUrl, post.instagramCaption);

                    if (igPostId != null) {
                        Thread.sleep(1500); // Give Meta a moment to index
                        String permalink = instagramPublisherService.getPostPermalink(igPostId);

                        // 3. SAVE VAST INFORMATION TO DATABASE
                        MasterclassPost entity = new MasterclassPost();
                        entity.setTopic(post.topic);
                        entity.setInstagramCaption(post.instagramCaption);
                        entity.setVastInformation(post.vastInformation); // The long blog part
                        entity.setImageUrl(publicUrl);
                        entity.setInstagramPermalink(permalink);
                        postRepository.save(entity);

                        // 4. Success Response
                        String webUrl = "http://localhost:4200/post/" + entity.getId();
                        updateStatus(chatId, messageId,
                                "✅ *LIVE ON INSTAGRAM & WEB!*\n\n" +
                                        "🔗 [Instagram Post](" + permalink + ")\n" +
                                        "📖 [Full Web Masterclass](" + webUrl + ")");
                    } else {
                        updateStatus(chatId, messageId, "❌ Instagram API rejected the post.");
                    }
                }
            } catch (Exception e) {
                updateStatus(chatId, messageId, "⚠️ Error: " + e.getMessage());
            } finally {
                pendingPosts.remove(chatId);
            }
        }
    }

    private void updateStatus(long chatId, int msgId, String text) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(msgId);
        edit.setText(text);
        edit.setParseMode("Markdown");
        try { execute(edit); } catch (Exception e) {}
    }

    private void sendText(long chatId, String text) {
        SendMessage sm = new SendMessage(String.valueOf(chatId), text);
        sm.setParseMode("Markdown");
        try { execute(sm); } catch (Exception e) {}
    }

    public String makeInstagramReady(String publicUrl) {
        if (publicUrl == null) return null;

        // c_pad: Adds padding to reach the target size
        // ar_4:5: Forces a 4:5 Portrait aspect ratio (best for long code)
        // w_1080: Sets standard Instagram width
        // b_rgb:1e1e1e: Matches the dark code editor background
        return publicUrl.replace("/upload/", "/upload/c_pad,ar_4:5,w_1080,b_rgb:1e1e1e/");
    }

    // UPDATED PendingPost structure to support Blog content
    private static class PendingPost {
        byte[] imageBytes;
        String instagramCaption;
        String vastInformation;
        String topic;

        PendingPost(byte[] b, String ic, String vi, String t) {
            this.imageBytes = b;
            this.instagramCaption = ic;
            this.vastInformation = vi;
            this.topic = t;
        }
    }
}
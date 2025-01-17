package com.bot.commands.nsfw;

import com.bot.caching.R34Cache;
import com.bot.commands.NSFWCommand;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.ChannelType;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rule34Command extends NSFWCommand {
    private Random random;
    private R34Cache cache;

    public Rule34Command() {
        this.name = "r34";
        this.aliases = new String[]{"rule34"};
        this.arguments = "<tags to search for>";
        this.cooldown = 1;
        this.help = "Gets rule 34 for the given tags";

        this.random = new Random(System.currentTimeMillis());
        this.cache = R34Cache.getInstance();
    }

    @Override
    protected void executeCommand(CommandEvent commandEvent) {
        // Get the tags
        String r34url = "http://rule34.xxx/index.php?page=dapi&s=post&q=index&limit=200&tags=" + commandEvent.getArgs();
        String booruUrl = "https://yande.re/post.xml?tags=" + commandEvent.getArgs();
        List<String> imageUrls = cache.get(commandEvent.getArgs());

        try {
            if (imageUrls == null) {
                if (!commandEvent.isFromType(ChannelType.PRIVATE))
                    commandEvent.getTextChannel().sendTyping().queue();
                imageUrls = new ArrayList<>();
                imageUrls.addAll(getImageURLFromSearch(r34url));
                imageUrls.addAll(getImageURLFromSearch(booruUrl));
                cache.put(commandEvent.getArgs(), imageUrls);
            }
            String selected = imageUrls.get(random.nextInt(imageUrls.size()));
            commandEvent.reply(selected);
        } catch (IllegalArgumentException e) {
            commandEvent.reply(commandEvent.getClient().getWarning() + " I couldn't find any results for that search.");
        } catch (Exception e) {
            logger.severe("Something went wrong getting r34 post: ", e);
            commandEvent.reply(commandEvent.getClient().getError() + " Something went wrong getting the image, please try again.");
            metricsManager.markCommandFailed(this, commandEvent.getAuthor(), commandEvent.getGuild());
        }
    }

    private List<String> getImageURLFromSearch(String url) throws Exception{
        HttpGet get = new HttpGet(url);
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            ResponseHandler<String> responseHandler = response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            };

            String responseBody = client.execute(get, responseHandler);
            client.close();

            // Regex the returned xml and get all links
            Pattern expression = Pattern.compile("(file_url)=[\"']?((?:.(?![\"']?\\s+(?:\\S+)=|[>\"']))+.)[\"']?");
            Matcher matcher = expression.matcher(responseBody);
            ArrayList<String> possibleLinks = new ArrayList<>();

            while (matcher.find()) {
                // Add the second group of regex
                possibleLinks.add(matcher.group(2));
            }

            return possibleLinks;
        }
    }
}

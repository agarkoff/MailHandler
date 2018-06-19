package ru.mailhandler;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.misterparser.common.MisterParserFileUtils;
import ru.misterparser.common.configuration.ConfigurationUtils;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 22.12.16
 * Time: 12:28
 */
public class TemplateManager {

    private static final Logger log = LogManager.getLogger(TemplateManager.class);

    private static final Pattern PRIORITY_PATTERN = Pattern.compile("%PRIORITY\\s*=\\s*\"\\s*(\\w)\\s*\"");

    public String format(String template, String oFromFName, Date date, String quotes, String newText) {
        //template = template.replace("\n", "\n<br/>");
        template = template.replace("%OFromFName", oFromFName);
        if (date != null) {
            String dateFormatted = DateFormat.getDateInstance(DateFormat.FULL).format(date);
            template = template.replace("%ODateEn", dateFormatted);
            String timeFormatted = DateFormat.getTimeInstance(DateFormat.DEFAULT).format(date);
            template = template.replace("%OTimeLongEn", timeFormatted);
        }
//        List<String> quotesLines = new ArrayList<>();
//        for (String q : StringUtils.split(quotes, "\n")) {
//            quotesLines.add("> " + q);
//        }
//        quotes = StringUtils.join(quotesLines, "\n");
        template = template.replace("%Quotes", "<blockquote>" + quotes + "</blockquote>");

        String sign = MisterParserFileUtils.readFileToStringQuietly(new File(ConfigurationUtils.getCurrentDirectory() + "sign.html"), "UTF-8");
        if (StringUtils.isNotBlank(sign)) {
            //newText += "\n<br/>-- \n<br/>";
            newText += "<br/>" + sign;
        }

        template = template.replace("%Cursor", newText);
        template = template.replaceAll(PRIORITY_PATTERN.toString(), "");
        template = template.replace("\r", "");

//        try {
//            FileUtils.write(new File(ConfigurationUtils.getCurrentDirectory() + "/debug/" + System.currentTimeMillis() + ".html"), template, "UTF-8");
//        } catch (IOException e) {
//            log.debug("IOException", e);
//        }

        return template;
    }

    public int getPriority(String template) {
        int r = 3;
        Matcher matcher = PRIORITY_PATTERN.matcher(template);
        if (matcher.find()) {
            String p = matcher.group(1);
            if (StringUtils.equalsIgnoreCase(p, "H")) {
                r = 1;
            } else if (StringUtils.equalsIgnoreCase(p, "N")) {
                r = 3;
            } else if (StringUtils.equalsIgnoreCase(p, "L")) {
                r = 5;
            } else {
                throw new RuntimeException("Ошибка в приоритете: " + p + ". Допустимые значения: H, N, L");
            }
        }
        return r;
    }
}

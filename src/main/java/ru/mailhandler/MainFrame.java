package ru.mailhandler;

import com.google.gson.Gson;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.sun.management.OperatingSystemMXBean;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javatuples.Pair;
import org.jdesktop.swingx.JXLoginPane;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.auth.LoginService;
import org.jdesktop.swingx.renderer.DefaultListRenderer;
import ru.mailhandler.db.Database;
import ru.mailhandler.model.Proxy;
import ru.mailhandler.model.SentMessage;
import ru.mailhandler.settings.Folder;
import ru.mailhandler.settings.Settings;
import ru.mailhandler.stat.FetchEmailIntervalStats;
import ru.mailhandler.stat.Stats;
import ru.mailhandler.stat.StatsController;
import ru.mailhandler.thread.Main;
import ru.mailhandler.thread.MessageSender;
import ru.mailhandler.thread.ProxyCleaner;
import ru.misterparser.common.Utils;
import ru.misterparser.common.configuration.ConfigurationUtils;
import ru.misterparser.common.gui.BfDateTimePicker;
import ru.misterparser.common.gui.ContextMenuEventListener;
import ru.misterparser.common.gui.GuiUtils;
import ru.misterparser.common.gui.combobox.ComboBoxUtils;
import ru.misterparser.common.gui.combobox.FontChooserComboBox;
import ru.misterparser.common.gui.list.ListUtils;
import ru.misterparser.common.gui.list.NamedElementListCellRenderer;
import ru.misterparser.common.logging.TextAreaAppender;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.ProxySelector;
import java.util.*;
import java.util.List;
import java.util.Timer;

/**
 * Created with IntelliJ IDEA.
 * User: Stas
 * Date: 17.12.16
 * Time: 18:14
 */
public class MainFrame {

    private static final Logger log = LogManager.getLogger(MainFrame.class);

    private static final String FRAME_TITLE = "MailHandler";

    private JFrame frame;
    private JPanel rootPanel;
    private JTabbedPane tabbedPane1;
    private JButton actionButton;
    private JButton threadControlButton;
    private JTextArea logTextArea;
    private JScrollPane logScrollPane;
    private JTextField pop3EmailLoginTextField;
    private JTextField pop3HostTextField;
    private JSpinner pop3PortSpinner;
    private JTextField smtpHostTextField;
    private JSpinner smtpPortSpinner;
    private JSpinner pop3MailboxConnectionTimeoutSpinner;
    private JTextField proxyPanelUserTextField;
    private JPasswordField pop3EmailPasswordField;
    private JPasswordField proxyPanelPassField;
    private JSpinner proxyTtlSpinner;
    private JSpinner maxCountProxyTodaySpinner;
    private JSpinner proxyMaxErrorCountSpinner;
    private JTextArea proxyPanelUrlsTextArea;
    private JSpinner fetchEmailIntervalSpinner;
    private JSpinner sendingDelaySpinner;
    private JTextField adminEmailTextField;
    private JSpinner minResponseIntervalSpinner;
    private JTextArea messageTemplateTextArea;
    private JPanel minResponseIntervalPanel;
    private JCheckBox responseTimeFilterCheckBox;
    private JCheckBox deleteBySubjectAndFromFilterCheckBox;
    private JPanel deleteBySubjectAndFromFilterPanel;
    private JTextArea deleteBySubjectAndFromFilterTagsTextArea;
    private JTextArea emailForResponseFinderFilenameTagsTextArea;
    private JList<String> emailForResponseFinderExtensionsList;
    private JList<Folder> folderList;
    private JButton folderChangeButton;
    private JButton folderDeleteButton;
    private JButton folderAddButton;
    private JTextField countReceivedEmailByHourTextField;
    private JTextField countSentEmailByHourTextField;
    private JSpinner timeoutBetweenMessageSendSpinner;
    private JProgressBar cpuLoadProgressBar;
    private JProgressBar messageSenderProgressBar;
    private JTextField countReceivedEmailTodayTextField;
    private JTextField countSentEmailTodayTextField;
    private JTextField undeliveredTodayTextField;
    private JCheckBox disableEmailSearchInFromCheckBox;
    private JCheckBox disableEmailSearchInBodyCheckBox;
    private JCheckBox disableEmailSearchInAttachmentsCheckBox;
    private JTextField usedProxyTodayTextField;
    private JTextField countDownloadedProxyTodayTextField;
    private JComboBox<Proxy> socksComboBox;
    private JXTable categoryTable;
    private JComboBox<String> monday1ComboBox;
    private JComboBox<String> monday2ComboBox;
    private JComboBox<String> tuesday1ComboBox;
    private JComboBox<String> tuesday2ComboBox;
    private JComboBox<String> wednesday1ComboBox;
    private JComboBox<String> wednesday2ComboBox;
    private JComboBox<String> thursday1ComboBox;
    private JComboBox<String> thursday2ComboBox;
    private JComboBox<String> friday1ComboBox;
    private JComboBox<String> friday2ComboBox;
    private JComboBox<String> saturday1ComboBox;
    private JComboBox<String> saturday2ComboBox;
    private JComboBox<String> sunday1ComboBox;
    private JComboBox<String> sunday2ComboBox;
    private JCheckBox mondayCheckBox;
    private JCheckBox tuesdayCheckBox;
    private JCheckBox wednesdayCheckBox;
    private JCheckBox thursdayCheckBox;
    private JCheckBox fridayCheckBox;
    private JCheckBox saturdayCheckBox;
    private JCheckBox sundayCheckBox;
    private JComboBox<String> todayClockComboBox;
    private JSpinner socksThresholdSpinner;
    private JSlider speedSlider;
    private JPanel yellowPanel;
    private JPanel greenPanel;
    private JLabel greenLabel;
    private JLabel yellowLabel;
    private JCheckBox antiDeleteBySubjectAndFromFilterCheckBox;
    private JTextArea antiDeleteBySubjectAndFromFilterTagsTextArea;
    private BfDateTimePicker sentMessages1DateTimePicker;
    private BfDateTimePicker sentMessages2DateTimePicker;
    private JButton saveSentMessagesButton;
    private JTextField keyTextField;
    private JButton saveKeyButton;
    private JTextField repeatedSubjectTextField;
    private FontChooserComboBox logFontChooserComboBox;
    private JTabbedPane tabbedPane2;
    private JPanel antiDeleteBySubjectAndFromFilterPanel;
    private JTextField senderNameTextField;
    private JSpinner minResponseIntervalFirstSpinner;
    private JSpinner minResponseIntervalLastSpinner;
    private JTabbedPane tabbedPane3;
    private JTextField smtpEmailLoginTextField;
    private JPasswordField smtpEmailPasswordField;
    private JSpinner smtpMailboxConnectionTimeoutSpinner;
    private JCheckBox smtpSslCheckBox;
    private JTextField senderFromTextField;
    private JButton setDefaultFolderButton;
    private JLabel localePreviewLabel;
    private JComboBox localeComboBox;
    private JTextField replyToTextField;
    private JSpinner onceMessageCountSpinner;

    private static MainFrame mainFrame;

    private String currentDirectory;
    private Main main;
    private Thread mainThread;
    private ProxyCleaner proxyCleaner;
    private Thread proxyCleanerThread;
    private MessageSender messageSender;
    private Thread messageSenderThread;
    private boolean isStarted = false;
    private FolderListModel folderListModel = new FolderListModel();
    private CategoryTableModel categoryTableModel;
    private FetchEmailIntervalStats fetchEmailIntervalStats = new FetchEmailIntervalStats();

    private ActionListener actionButtonListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (isStarted) {
                mainThread.interrupt();
                proxyCleanerThread.interrupt();
                messageSenderThread.interrupt();
            } else {
                try {
                    main = new Main();
                    mainThread = new Thread(main, "mainThread");
                    mainThread.start();
                    proxyCleaner = new ProxyCleaner();
                    proxyCleanerThread = new Thread(proxyCleaner, "proxyCleanerThread");
                    proxyCleanerThread.start();
                    messageSender = new MessageSender(MainFrame.this);
                    messageSenderThread = new Thread(messageSender, "messageSenderThread");
                    messageSenderThread.start();
                    actionButton.setText("Стоп");
                    threadControlButton.setEnabled(true);
                    isStarted = true;
                } catch (Exception ex) {
                    log.debug("Exception", ex);
                    JOptionPane.showMessageDialog(frame, "Ошибка запуска потока\n" + ExceptionUtils.getFullStackTrace(ex), FRAME_TITLE, JOptionPane.ERROR_MESSAGE);
                    resetButtonState();
                }
            }
        }
    };

    private ActionListener threadControlButtonListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (main.isSuspended()) {
                main.resume();
                proxyCleaner.resume();
                messageSender.resume();
                threadControlButton.setText("Пауза");
            } else {
                main.suspend();
                proxyCleaner.suspend();
                messageSender.suspend();
                threadControlButton.setText("Продолжить");
            }
        }
    };

    private ActionListener saveSentMessagesButtonListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            new Thread() {
                @Override
                public void run() {
                    String filename = GuiUtils.getFilename(frame, "Выбери файл для сохранения", FileDialog.SAVE, "txt", Settings.get().DIRECTORY_HOLDER);
                    if (StringUtils.isNotBlank(filename)) {
                        try {
                            Date date1 = sentMessages1DateTimePicker.getValue();
                            Date date2 = sentMessages1DateTimePicker.getValue();
                            List<SentMessage> sentMessages = Database.get().getSentMessageDao().getSentMessages(date1, date2);
                            List<String> strings = new ArrayList<>();
                            for (SentMessage sentMessage : sentMessages) {
                                strings.add(sentMessage.getName() + " <" + sentMessage.getEmail() + ">");
                            }
                            FileUtils.writeLines(new File(filename), "UTF-8", strings);
                            JOptionPane.showMessageDialog(frame, "Сохранено " + sentMessages.size() + " строк", "Сохранение завершено", JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception e) {
                            log.debug("Exception", e);
                        }
                    }
                }
            }.start();
        }
    };

    private ActionListener saveKeyButtonListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (StringUtils.isNotBlank(keyTextField.getText())) {
                    byte[] n = keyTextField.getText().getBytes("UTF-8");
                    for (int i = 0; i < n.length; i++) {
                        n[i] ^= 0xE3;
                    }
                    keyTextField.setText("");
                    FileUtils.writeByteArrayToFile(new File(ConfigurationUtils.getCurrentDirectory() + "key"), n);
                    JOptionPane.showMessageDialog(frame, "Пароль обновлён", "Обновление пароля", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(frame, "Введите новый пароль", "Обновление пароля", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Throwable t) {
                log.debug("Throwable", t);
            }
        }
    };

    private ActionListener setDefaultFolderButtonListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (folderList.getSelectedValue() != null) {
                folderListModel.setDefaultFlag(folderList.getSelectedValue(), !folderList.getSelectedValue().isDefault());
                folderList.updateUI();
            }
        }
    };

    public void resetButtonState() {
        isStarted = false;
        actionButton.setText("Старт");
        threadControlButton.setText("Пауза");
        threadControlButton.setEnabled(false);
    }

    public MainFrame(String currentDirectory, @SuppressWarnings("UnusedParameters") String[] args) {
        this.currentDirectory = currentDirectory;
        $$$setupUI$$$();
        ConfigurationUtils.setCurrentDirectory(currentDirectory);
        //login();
    }

    private void login() {
        JXLoginPane loginPane = new JXLoginPane();
        JXLoginPane.Status jxLoginStatus = JXLoginPane.showLoginDialog(loginPane, new LoginService() {
            @Override
            public boolean authenticate(String name, char[] password, String server) throws Exception {
                byte[] b;
                try {
                    b = FileUtils.readFileToByteArray(new File(ConfigurationUtils.getCurrentDirectory() + "key"));
                } catch (Throwable t) {
                    b = new byte[]{-126, -121, -114, -118, -115};
                    FileUtils.writeByteArrayToFile(new File(ConfigurationUtils.getCurrentDirectory() + "key"), b);
                }
                byte[] n = name.getBytes("UTF-8");
                for (int i = 0; i < n.length; i++) {
                    n[i] ^= 0xE3;
                }
                return Arrays.equals(b, n);
            }
        });
        if (jxLoginStatus != JXLoginPane.Status.SUCCEEDED) {
            System.exit(0);
        }
    }

    public static void main(String[] args) throws ClassNotFoundException {
        System.setProperty("java.net.preferIPv4Stack", "true");
        Class.forName("org.sqlite.JDBC");
        String currentDirectoryTemp = ".";
        if (args.length > 0) {
            currentDirectoryTemp = args[0];
        }
        currentDirectoryTemp += System.getProperty("file.separator");
        final String currentDirectory = currentDirectoryTemp;
        final MainFrame mainFrame = new MainFrame(currentDirectory, args);
        MainFrame.mainFrame = mainFrame;
        mainFrame.start();
    }

    private void start() {
        try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    createAndShowGUI();
                }
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Ошибка создания окна\n" + ExceptionUtils.getFullStackTrace(e), FRAME_TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createAndShowGUI() {
        Toolkit.getDefaultToolkit().addAWTEventListener(new ContextMenuEventListener(), AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);

        frame = new JFrame(FRAME_TITLE);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(rootPanel);
        Dimension minimumSize = new Dimension(1100, 800);
        frame.setMinimumSize(minimumSize);
        frame.setSize(minimumSize);

        GuiUtils.updateUIOnPanel(rootPanel);
        GuiUtils.frameDisplayCenter(frame);
        GuiUtils.setupFrameIconImage(frame);

        frame.setVisible(true);

        actionButton.addActionListener(actionButtonListener);
        threadControlButton.addActionListener(threadControlButtonListener);
        saveSentMessagesButton.addActionListener(saveSentMessagesButtonListener);
        saveKeyButton.addActionListener(saveKeyButtonListener);
        setDefaultFolderButton.addActionListener(setDefaultFolderButtonListener);

        folderList.setModel(folderListModel);

        TextAreaAppender.setTextArea(logTextArea);
        GuiUtils.setupSearchByKeyboard(logTextArea);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                focusAdapter.focusLost(null);
                Database.get().closeQuietly();
            }
        });

        ProxySelector.setDefault(new CustomProxySelector());

        fillClockComboBox(monday1ComboBox);
        fillClockComboBox(monday2ComboBox);
        fillClockComboBox(tuesday1ComboBox);
        fillClockComboBox(tuesday2ComboBox);
        fillClockComboBox(wednesday1ComboBox);
        fillClockComboBox(wednesday2ComboBox);
        fillClockComboBox(thursday1ComboBox);
        fillClockComboBox(thursday2ComboBox);
        fillClockComboBox(friday1ComboBox);
        fillClockComboBox(friday2ComboBox);
        fillClockComboBox(saturday1ComboBox);
        fillClockComboBox(saturday2ComboBox);
        fillClockComboBox(sunday1ComboBox);
        fillClockComboBox(sunday2ComboBox);

        fillClockComboBox(todayClockComboBox);

        initLocaleCombobox(localeComboBox);
        loadSettings();
        loadStats();

        StatsController.get();

        pop3EmailLoginTextField.addFocusListener(focusAdapter);
        pop3EmailPasswordField.addFocusListener(focusAdapter);
        pop3HostTextField.addFocusListener(focusAdapter);
        pop3PortSpinner.addChangeListener(changeListener);
        pop3MailboxConnectionTimeoutSpinner.addChangeListener(changeListener);

        smtpEmailLoginTextField.addFocusListener(focusAdapter);
        smtpEmailPasswordField.addFocusListener(focusAdapter);
        smtpHostTextField.addFocusListener(focusAdapter);
        smtpPortSpinner.addChangeListener(changeListener);
        smtpSslCheckBox.addChangeListener(changeListener);
        smtpMailboxConnectionTimeoutSpinner.addChangeListener(changeListener);
        senderFromTextField.addFocusListener(focusAdapter);

        proxyPanelUserTextField.addFocusListener(focusAdapter);
        proxyPanelPassField.addFocusListener(focusAdapter);
        proxyTtlSpinner.addChangeListener(changeListener);
        maxCountProxyTodaySpinner.addChangeListener(changeListener);
        proxyMaxErrorCountSpinner.addChangeListener(changeListener);
        proxyPanelUrlsTextArea.addFocusListener(focusAdapter);
        fetchEmailIntervalSpinner.addChangeListener(changeListener);
        sendingDelaySpinner.addChangeListener(changeListener);
        adminEmailTextField.addFocusListener(focusAdapter);
        messageTemplateTextArea.addFocusListener(focusAdapter);
        responseTimeFilterCheckBox.addFocusListener(focusAdapter);
        responseTimeFilterCheckBox.addActionListener(updateFilterPanelState);
        minResponseIntervalSpinner.addChangeListener(changeListener);
        deleteBySubjectAndFromFilterCheckBox.addFocusListener(focusAdapter);
        deleteBySubjectAndFromFilterCheckBox.addActionListener(updateFilterPanelState);
        deleteBySubjectAndFromFilterTagsTextArea.addFocusListener(focusAdapter);
        antiDeleteBySubjectAndFromFilterCheckBox.addFocusListener(focusAdapter);
        antiDeleteBySubjectAndFromFilterCheckBox.addActionListener(updateFilterPanelState);
        antiDeleteBySubjectAndFromFilterTagsTextArea.addFocusListener(focusAdapter);
        emailForResponseFinderFilenameTagsTextArea.addFocusListener(focusAdapter);
        emailForResponseFinderExtensionsList.addFocusListener(focusAdapter);
        ListUtils.initDragAndDrop(emailForResponseFinderExtensionsList);
        folderList.addFocusListener(focusAdapter);
        ListUtils.initDragAndDrop(folderList);
        folderList.setCellRenderer(folderRenderer);
        folderAddButton.addActionListener(folderAddButtonListener);
        folderChangeButton.addActionListener(folderChangeButtonListener);
        folderDeleteButton.addActionListener(folderDeleteButtonListener);
        timeoutBetweenMessageSendSpinner.addChangeListener(changeListener);
        onceMessageCountSpinner.addChangeListener(changeListener);
        disableEmailSearchInFromCheckBox.addFocusListener(focusAdapter);
        disableEmailSearchInBodyCheckBox.addFocusListener(focusAdapter);
        disableEmailSearchInAttachmentsCheckBox.addFocusListener(focusAdapter);
        addAction(mondayCheckBox, monday1ComboBox, monday2ComboBox);
        addAction(tuesdayCheckBox, tuesday1ComboBox, tuesday2ComboBox);
        addAction(wednesdayCheckBox, wednesday1ComboBox, wednesday2ComboBox);
        addAction(thursdayCheckBox, thursday1ComboBox, thursday2ComboBox);
        addAction(fridayCheckBox, friday1ComboBox, friday2ComboBox);
        addAction(saturdayCheckBox, saturday1ComboBox, saturday2ComboBox);
        addAction(sundayCheckBox, sunday1ComboBox, sunday2ComboBox);
        todayClockComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
                StatsController.get().restartTodayCleaner();
            }
        });
        socksThresholdSpinner.addChangeListener(changeListener);
        speedSlider.addFocusListener(focusAdapter);
        speedSlider.addChangeListener(changeListener);
        repeatedSubjectTextField.addFocusListener(focusAdapter);
        logFontChooserComboBox.addFocusListener(focusAdapter);
        logFontChooserComboBox.addActionListener(actionListener);
        senderNameTextField.addFocusListener(focusAdapter);
        localeComboBox.addFocusListener(focusAdapter);
        localeComboBox.addActionListener(actionListener);
        replyToTextField.addFocusListener(focusAdapter);

        updateFilterPanelState.actionPerformed(null);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                    double d = operatingSystemMXBean.getProcessCpuLoad();
                    cpuLoadProgressBar.setValue((int) (d * 100));
                } catch (Throwable t) {
                    log.debug("Throwable", t);
                }
            }
        }, 0, 1000);

        initLed(yellowLabel, "/yellow.png");
        initLed(greenLabel, "/green.png");

        frame.setVisible(true);
//        new Timer().schedule(new TimerTask() {
//
//            Random random = new Random();
//
//            @Override
//            public void run() {
//                try {
//                    ArrayList<Folder> list = new ArrayList<>(Settings.get().FOLDERS);
//                    Folder folder = list.get(random.nextInt(list.size()));
//                    Stats.get().incCategoryStats(folder.getTitle());
//                } catch (Throwable t) {
//                    log.debug("Throwable", t);
//                }
//            }
//        }, 0, 1000);

        actionButton.doClick();
    }

    private NamedElementListCellRenderer folderRenderer = new NamedElementListCellRenderer() {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            try {
                Folder folder = (Folder) value;
                if (folder.isDefault()) {
                    Font font = label.getFont();
                    Font boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
                    label.setFont(boldFont);
                }
            } catch (Throwable t) {
                log.debug("Throwable", t);
            }
            return label;
        }
    };

    private void initLed(JLabel label, String resourceName) {
        try {
            BufferedImage image = ImageIO.read(getClass().getResourceAsStream(resourceName));
            Image resized = image.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(resized));
            label.setVisible(false);
        } catch (Exception e) {
            log.debug("Exception", e);
        }
    }

    private void addAction(JCheckBox checkBox, JComboBox comboBox1, JComboBox comboBox2) {
        checkBox.addActionListener(actionListener);
        comboBox1.addActionListener(actionListener);
        comboBox2.addActionListener(actionListener);
    }

    private void fillClockComboBox(JComboBox<String> comboBox) {
        for (int i = 0; i < 24; i++) {
            comboBox.addItem(String.format("%02d", i) + ":00");
            comboBox.addItem(String.format("%02d", i) + ":30");
        }
    }

    private void initLocaleCombobox(JComboBox<Locale> comboBox) {
        //noinspection unchecked
        comboBox.setRenderer(new DefaultListRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Locale locale = (Locale) value;
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setText(locale.getDisplayName(Locale.forLanguageTag("RU")));
                return label;
            }
        });
        List<Locale> locales = new ArrayList<>(Arrays.asList(Locale.getAvailableLocales()));
        Collections.sort(locales, new Comparator<Locale>() {
            @Override
            public int compare(Locale o1, Locale o2) {
                return o1.getDisplayName(Locale.forLanguageTag("RU")).compareToIgnoreCase(o2.getDisplayName(Locale.forLanguageTag("RU")));
            }
        });
        for (Locale locale : locales) {
            comboBox.addItem(locale);
        }
        localeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Locale.setDefault((Locale) localeComboBox.getSelectedItem());
                Helpers.updateLocalePreview(localePreviewLabel);
            }
        });
    }
    private void loadSettings() {
        try {
            String s = Utils.readFileAsString(new File(ConfigurationUtils.getCurrentDirectory() + "settings.json"), "UTF-8");
            Gson gson = new Gson();
            Settings.setSettings(gson.fromJson(s, Settings.class));
        } catch (Throwable t) {
            log.debug("Throwable", t);
            Settings.get().EMAIL_FOR_RESPONSE_FINDER_EXTENSIONS = new ArrayList<>(Arrays.asList("pdf", "txt", "doc", "docx", "xls", "xlsx", "rtf", "html", "htm"));
        }
        pop3EmailLoginTextField.setText(Settings.get().POP3_EMAIL_LOGIN);
        pop3EmailPasswordField.setText(Settings.get().POP3_EMAIL_PASSWORD);
        pop3HostTextField.setText(Settings.get().POP3_HOST);
        pop3PortSpinner.setValue(Settings.get().POP3_PORT);
        pop3MailboxConnectionTimeoutSpinner.setValue(Settings.get().POP3_MAILBOX_CONNECTION_TIMEOUT / 1000);

        smtpEmailLoginTextField.setText(Settings.get().SMTP_EMAIL_LOGIN);
        smtpEmailPasswordField.setText(Settings.get().SMTP_EMAIL_PASSWORD);
        smtpHostTextField.setText(Settings.get().SMTP_HOST);
        smtpPortSpinner.setValue(Settings.get().SMTP_PORT);
        smtpSslCheckBox.setSelected(Settings.get().SMTP_SSL);
        smtpMailboxConnectionTimeoutSpinner.setValue(Settings.get().SMTP_MAILBOX_CONNECTION_TIMEOUT / 1000);
        senderFromTextField.setText(Settings.get().SENDER_FROM);

        proxyPanelUserTextField.setText(Settings.get().PROXY_PANEL_USER);
        proxyPanelPassField.setText(Settings.get().PROXY_PANEL_PASS);
        proxyTtlSpinner.setValue(Settings.get().PROXY_TTL / 1000);
        maxCountProxyTodaySpinner.setValue(Settings.get().MAX_COUNT_PROXY_TODAY);
        proxyMaxErrorCountSpinner.setValue(Settings.get().PROXY_MAX_ERROR_COUNT);
        proxyPanelUrlsTextArea.setText(StringUtils.join(Settings.get().PROXY_PANEL_URLS, "\n"));
        fetchEmailIntervalSpinner.setValue(Settings.get().FETCH_EMAIL_INTERVAL / 1000);
        sendingDelaySpinner.setValue(Settings.get().SENDING_DELAY / 1000);
        adminEmailTextField.setText(Settings.get().ADMIN_EMAIL);
        messageTemplateTextArea.setText(Settings.get().MESSAGE_TEMPLATE);
        responseTimeFilterCheckBox.setSelected(Settings.get().RESPONSE_TIME_FILTER);
        minResponseIntervalSpinner.setValue(Settings.get().MIN_RESPONSE_INTERVAL / 1000);
        minResponseIntervalFirstSpinner.setValue(Settings.get().MIN_RESPONSE_INTERVAL_FIRST);
        minResponseIntervalLastSpinner.setValue(Settings.get().MIN_RESPONSE_INTERVAL_LAST);
        deleteBySubjectAndFromFilterCheckBox.setSelected(Settings.get().DELETE_BY_SUBJECT_AND_FROM_FILTER);
        deleteBySubjectAndFromFilterTagsTextArea.setText(StringUtils.join(Settings.get().DELETE_BY_SUBJECT_AND_FROM_FILTER_TAGS, "\n"));
        antiDeleteBySubjectAndFromFilterCheckBox.setSelected(Settings.get().ANTI_DELETE_BY_SUBJECT_AND_FROM_FILTER);
        antiDeleteBySubjectAndFromFilterTagsTextArea.setText(StringUtils.join(Settings.get().ANTI_DELETE_BY_SUBJECT_AND_FROM_FILTER_TAGS, "\n"));
        emailForResponseFinderFilenameTagsTextArea.setText(StringUtils.join(Settings.get().EMAIL_FOR_RESPONSE_FINDER_FILENAME_TAGS, "\n"));
        ListUtils.setList(emailForResponseFinderExtensionsList, Settings.get().EMAIL_FOR_RESPONSE_FINDER_EXTENSIONS);
        folderListModel.setCollection(Settings.get().FOLDERS);
        if (folderListModel.size() > 0) {
            folderList.setSelectedIndex(0);
        }
        timeoutBetweenMessageSendSpinner.setValue(Settings.get().TIMEOUT_BETWEEN_MESSAGE_SEND / 1000);
        onceMessageCountSpinner.setValue(Settings.get().ONCE_MESSAGE_COUNT);
        disableEmailSearchInFromCheckBox.setSelected(Settings.get().DISABLE_EMAIL_SEARCH_IN_FROM);
        disableEmailSearchInBodyCheckBox.setSelected(Settings.get().DISABLE_EMAIL_SEARCH_IN_BODY);
        disableEmailSearchInAttachmentsCheckBox.setSelected(Settings.get().DISABLE_EMAIL_SEARCH_IN_ATTACHMENTS);
        refreshSocksCombobox();
        socksComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshSocksCombobox();
            }
        });
        fillTimeBlock(mondayCheckBox, monday1ComboBox, monday2ComboBox, Settings.get().MONDAY_CLOCK);
        fillTimeBlock(tuesdayCheckBox, tuesday1ComboBox, tuesday2ComboBox, Settings.get().TUESDAY_CLOCK);
        fillTimeBlock(wednesdayCheckBox, wednesday1ComboBox, wednesday2ComboBox, Settings.get().WEDNESDAY_CLOCK);
        fillTimeBlock(thursdayCheckBox, thursday1ComboBox, thursday2ComboBox, Settings.get().THURSDAY_CLOCK);
        fillTimeBlock(fridayCheckBox, friday1ComboBox, friday2ComboBox, Settings.get().FRIDAY_CLOCK);
        fillTimeBlock(saturdayCheckBox, saturday1ComboBox, saturday2ComboBox, Settings.get().SATURDAY_CLOCK);
        fillTimeBlock(sundayCheckBox, sunday1ComboBox, sunday2ComboBox, Settings.get().SUNDAY_CLOCK);
        todayClockComboBox.setSelectedItem(Helpers.formatTime(Settings.get().TODAY_CLOCK));
        socksThresholdSpinner.setValue(Settings.get().SOCKS_THRESHOLD);
        speedSlider.setValue(Settings.get().SPEED);
        repeatedSubjectTextField.setText(Settings.get().REPEATED_SUBJECT);
        logFontChooserComboBox.setSelectedItem(Settings.get().LOG_FONT_NAME);
        logTextArea.setFont(new Font(Settings.get().LOG_FONT_NAME, logTextArea.getFont().getStyle(), logTextArea.getFont().getSize()));
        senderNameTextField.setText(Settings.get().SENDER_NAME);
        localeComboBox.setSelectedItem(Helpers.findLocaleByName(Settings.get().LOCALE));
        replyToTextField.setText(Settings.get().REPLY_TO);
    }

    public void refreshSocksCombobox() {
        try {
            ComboBoxUtils.initComboBox(socksComboBox, Database.get().getProxyDao().queryForAll());
            new Thread() {
                @Override
                public void run() {
                    try {
                        if (socksComboBox.getModel().getSize() > 0) {
                            socksComboBox.setSelectedItem(SocksManager.get().getCurrentProxy());
                        }
                    } catch (Throwable t) {
                        log.debug("Throwable", t);
                    }
                }
            }.start();
        } catch (Throwable t) {
            log.debug("Throwable", t);
        }
    }

    private FocusAdapter focusAdapter = new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent e) {
            save();
        }
    };

    private ChangeListener changeListener = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            save();
        }
    };

    private ActionListener actionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            save();
        }
    };

    private void save() {
        try {
            Settings.get().POP3_EMAIL_LOGIN = pop3EmailLoginTextField.getText();
            Settings.get().POP3_EMAIL_PASSWORD = new String(pop3EmailPasswordField.getPassword());
            Settings.get().POP3_HOST = pop3HostTextField.getText();
            Settings.get().POP3_PORT = ((Number) pop3PortSpinner.getValue()).intValue();
            Settings.get().POP3_MAILBOX_CONNECTION_TIMEOUT = ((Number) pop3MailboxConnectionTimeoutSpinner.getValue()).longValue() * 1000;

            Settings.get().SMTP_EMAIL_LOGIN = smtpEmailLoginTextField.getText();
            Settings.get().SMTP_EMAIL_PASSWORD = new String(smtpEmailPasswordField.getPassword());
            Settings.get().SMTP_HOST = smtpHostTextField.getText();
            Settings.get().SMTP_PORT = ((Number) smtpPortSpinner.getValue()).intValue();
            Settings.get().SMTP_SSL = smtpSslCheckBox.isSelected();
            Settings.get().SMTP_MAILBOX_CONNECTION_TIMEOUT = ((Number) smtpMailboxConnectionTimeoutSpinner.getValue()).longValue() * 1000;
            Settings.get().SENDER_FROM = senderFromTextField.getText();

            Settings.get().PROXY_PANEL_USER = proxyPanelUserTextField.getText();
            Settings.get().PROXY_PANEL_PASS = new String(proxyPanelPassField.getPassword());
            Settings.get().PROXY_TTL = ((Number) proxyTtlSpinner.getValue()).longValue() * 1000;
            Settings.get().MAX_COUNT_PROXY_TODAY = ((Number) maxCountProxyTodaySpinner.getValue()).longValue();
            Settings.get().PROXY_MAX_ERROR_COUNT = ((Number) proxyMaxErrorCountSpinner.getValue()).intValue();
            Settings.get().PROXY_PANEL_URLS = new ArrayList<>(Arrays.asList(StringUtils.split(proxyPanelUrlsTextArea.getText(), "\n")));

            Settings.get().FETCH_EMAIL_INTERVAL = ((Number) fetchEmailIntervalSpinner.getValue()).longValue() * 1000;
            Settings.get().SENDING_DELAY = ((Number) sendingDelaySpinner.getValue()).longValue() * 1000;
            Settings.get().ADMIN_EMAIL = adminEmailTextField.getText();
            Settings.get().MESSAGE_TEMPLATE = messageTemplateTextArea.getText();
            Settings.get().RESPONSE_TIME_FILTER = responseTimeFilterCheckBox.isSelected();
            Settings.get().MIN_RESPONSE_INTERVAL = ((Number) minResponseIntervalSpinner.getValue()).longValue() * 1000;
            Settings.get().MIN_RESPONSE_INTERVAL_FIRST = ((Number) minResponseIntervalFirstSpinner.getValue()).intValue();
            Settings.get().MIN_RESPONSE_INTERVAL_LAST = ((Number) minResponseIntervalLastSpinner.getValue()).intValue();
            Settings.get().DELETE_BY_SUBJECT_AND_FROM_FILTER = deleteBySubjectAndFromFilterCheckBox.isSelected();
            Settings.get().DELETE_BY_SUBJECT_AND_FROM_FILTER_TAGS = new ArrayList<>(Arrays.asList(StringUtils.split(deleteBySubjectAndFromFilterTagsTextArea.getText(), "\n")));
            Settings.get().ANTI_DELETE_BY_SUBJECT_AND_FROM_FILTER = antiDeleteBySubjectAndFromFilterCheckBox.isSelected();
            Settings.get().ANTI_DELETE_BY_SUBJECT_AND_FROM_FILTER_TAGS = new ArrayList<>(Arrays.asList(StringUtils.split(antiDeleteBySubjectAndFromFilterTagsTextArea.getText(), "\n")));
            Settings.get().EMAIL_FOR_RESPONSE_FINDER_FILENAME_TAGS = new ArrayList<>(Arrays.asList(StringUtils.split(emailForResponseFinderFilenameTagsTextArea.getText(), "\n")));
            Settings.get().EMAIL_FOR_RESPONSE_FINDER_EXTENSIONS = new ArrayList(Arrays.asList(((DefaultListModel) emailForResponseFinderExtensionsList.getModel()).toArray()));
            Settings.get().FOLDERS = new LinkedHashSet(new ArrayList(Arrays.asList(((DefaultListModel) folderList.getModel()).toArray())));
            Settings.get().TIMEOUT_BETWEEN_MESSAGE_SEND = ((Number) timeoutBetweenMessageSendSpinner.getValue()).longValue() * 1000;
            Settings.get().ONCE_MESSAGE_COUNT = ((Number) onceMessageCountSpinner.getValue()).longValue();
            Settings.get().DISABLE_EMAIL_SEARCH_IN_FROM = disableEmailSearchInFromCheckBox.isSelected();
            Settings.get().DISABLE_EMAIL_SEARCH_IN_BODY = disableEmailSearchInBodyCheckBox.isSelected();
            Settings.get().DISABLE_EMAIL_SEARCH_IN_ATTACHMENTS = disableEmailSearchInAttachmentsCheckBox.isSelected();
            Settings.get().MONDAY_CLOCK = mondayCheckBox.isSelected() ? null : parseTime(monday1ComboBox, monday2ComboBox);
            Settings.get().TUESDAY_CLOCK = tuesdayCheckBox.isSelected() ? null : parseTime(tuesday1ComboBox, tuesday2ComboBox);
            Settings.get().WEDNESDAY_CLOCK = wednesdayCheckBox.isSelected() ? null : parseTime(wednesday1ComboBox, wednesday2ComboBox);
            Settings.get().THURSDAY_CLOCK = thursdayCheckBox.isSelected() ? null : parseTime(thursday1ComboBox, thursday2ComboBox);
            Settings.get().FRIDAY_CLOCK = fridayCheckBox.isSelected() ? null : parseTime(friday1ComboBox, friday2ComboBox);
            Settings.get().SATURDAY_CLOCK = saturdayCheckBox.isSelected() ? null : parseTime(saturday1ComboBox, saturday2ComboBox);
            Settings.get().SUNDAY_CLOCK = sundayCheckBox.isSelected() ? null : parseTime(sunday1ComboBox, sunday2ComboBox);
            Settings.get().TODAY_CLOCK = Helpers.parseTime((String) todayClockComboBox.getSelectedItem());
            Settings.get().SOCKS_THRESHOLD = ((Number) socksThresholdSpinner.getValue()).intValue();
            Settings.get().SPEED = speedSlider.getValue();
            Settings.get().REPEATED_SUBJECT = repeatedSubjectTextField.getText();
            Settings.get().LOG_FONT_NAME = StringUtils.substringBefore(logFontChooserComboBox.getSelectedFontName(), ".");
            logTextArea.setFont(new Font(Settings.get().LOG_FONT_NAME, logTextArea.getFont().getStyle(), logTextArea.getFont().getSize()));
            Settings.get().SENDER_NAME = senderNameTextField.getText();
            Settings.get().LOCALE = ((Locale) localeComboBox.getSelectedItem()).getDisplayName(Locale.forLanguageTag("RU"));
            Settings.get().REPLY_TO = replyToTextField.getText();
            log.debug("Settings updated");
            Settings.get().save();
            log.debug("Settings save to disk");
        } catch (Throwable t) {
            log.debug("Throwable", t);
        }
    }

    private Pair<Long, Long> parseTime(JComboBox<String> comboBox1, JComboBox<String> comboBox2) {
        return new Pair<>(Helpers.parseTime((String) comboBox1.getSelectedItem()), Helpers.parseTime((String) comboBox2.getSelectedItem()));
    }

    private void fillTimeBlock(JCheckBox checkBox, JComboBox comboBox1, JComboBox comboBox2, Pair<Long, Long> pair) {
        if (pair == null) {
            checkBox.setSelected(true);
        } else {
            comboBox1.setSelectedItem(Helpers.formatTime(pair.getValue0()));
            comboBox2.setSelectedItem(Helpers.formatTime(pair.getValue1()));
        }
    }

    private ActionListener updateFilterPanelState = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            GuiUtils.setEnabledRecursively(minResponseIntervalPanel, responseTimeFilterCheckBox.isSelected());
            GuiUtils.setEnabledRecursively(deleteBySubjectAndFromFilterPanel, deleteBySubjectAndFromFilterCheckBox.isSelected());
            GuiUtils.setEnabledRecursively(antiDeleteBySubjectAndFromFilterPanel, antiDeleteBySubjectAndFromFilterCheckBox.isSelected());
        }
    };

    private ActionListener folderAddButtonListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            FolderDialog folderDialog = new FolderDialog(Folder.NEW_CATEGORY.clone());
            folderDialog.show(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Folder folder = (Folder) e.getSource();
                    folderListModel.addElement(folder);
                    focusAdapter.focusLost(null);
                    folderList.setSelectedIndex(folderList.getModel().getSize() - 1);
                    Stats.get().addOrChangeCategoryStats(null, folder.getTitle());
                }
            }, folderListModel);
        }
    };

    private ActionListener folderChangeButtonListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            FolderDialog folderDialog = new FolderDialog(folderList.getSelectedValue());
            final String oldFolderTitle = folderList.getSelectedValue().getTitle();
            folderDialog.show(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Folder folder = (Folder) e.getSource();
                    folderList.updateUI();
                    focusAdapter.focusLost(null);
                    Stats.get().addOrChangeCategoryStats(oldFolderTitle, folder.getTitle());
                }
            }, folderListModel);
        }
    };

    private ActionListener folderDeleteButtonListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            final String oldFolderTitle = folderList.getSelectedValue().getTitle();
            int i = folderListModel.indexOf(folderList.getSelectedValue());
            folderListModel.removeElement(folderList.getSelectedValue());
            folderList.updateUI();
            focusAdapter.focusLost(null);
            if (i - 1 < folderList.getModel().getSize()) {
                folderList.setSelectedIndex(i - 1);
            }
            Stats.get().addOrChangeCategoryStats(oldFolderTitle, null);
        }
    };

    private void createUIComponents() {
        GuiUtils.initLAF();
        rootPanel = new JPanel();
    }

    private void loadStats() {
        try {
            String s = Utils.readFileAsString(new File(ConfigurationUtils.getCurrentDirectory() + "stats.json"), "UTF-8");
            Gson gson = new Gson();
            Stats.setStats(gson.fromJson(s, Stats.class));
        } catch (Throwable t) {
            log.debug("Throwable", t);
        }
        categoryTableModel = new CategoryTableModel(Stats.get().getCategoryStats());
        categoryTable.setModel(categoryTableModel);
        Stats.get().saveAndDisplayStats();
    }

    public JTextField getCountReceivedEmailByHourTextField() {
        return countReceivedEmailByHourTextField;
    }

    public JTextField getCountSentEmailByHourTextField() {
        return countSentEmailByHourTextField;
    }

    public JProgressBar getMessageSenderProgressBar() {
        return messageSenderProgressBar;
    }

    public JTextField getCountReceivedEmailTodayTextField() {
        return countReceivedEmailTodayTextField;
    }

    public JTextField getCountSentEmailTodayTextField() {
        return countSentEmailTodayTextField;
    }

    public JTextField getUndeliveredTodayTextField() {
        return undeliveredTodayTextField;
    }

    public JTextField getUsedProxyTodayTextField() {
        return usedProxyTodayTextField;
    }

    public JTextField getCountDownloadedProxyTodayTextField() {
        return countDownloadedProxyTodayTextField;
    }

    public CategoryTableModel getCategoryTableModel() {
        return categoryTableModel;
    }

    public static MainFrame get() {
        return mainFrame;
    }

    public JLabel getYellowLabel() {
        return yellowLabel;
    }

    public JLabel getGreenLabel() {
        return greenLabel;
    }

    public FetchEmailIntervalStats getFetchEmailIntervalStats() {
        return fetchEmailIntervalStats;
    }

    public void setFetchEmailIntervalStats(FetchEmailIntervalStats fetchEmailIntervalStats) {
        this.fetchEmailIntervalStats = fetchEmailIntervalStats;
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        rootPanel.setLayout(new GridLayoutManager(4, 5, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1 = new JTabbedPane();
        rootPanel.add(tabbedPane1, new GridConstraints(0, 0, 3, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("Статистика", panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(7, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Получено писем за час");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        countReceivedEmailByHourTextField = new JTextField();
        countReceivedEmailByHourTextField.setEditable(false);
        panel3.add(countReceivedEmailByHourTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Отвечено писем за час");
        panel3.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        countSentEmailByHourTextField = new JTextField();
        countSentEmailByHourTextField.setEditable(false);
        panel3.add(countSentEmailByHourTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Получено писем за СЕГОДНЯ");
        panel3.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        countReceivedEmailTodayTextField = new JTextField();
        countReceivedEmailTodayTextField.setEditable(false);
        panel3.add(countReceivedEmailTodayTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Отправлено писем за СЕГОДНЯ");
        panel3.add(label4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        countSentEmailTodayTextField = new JTextField();
        countSentEmailTodayTextField.setEditable(false);
        panel3.add(countSentEmailTodayTextField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Возвращено за СЕГОДНЯ");
        panel3.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        undeliveredTodayTextField = new JTextField();
        undeliveredTodayTextField.setEditable(false);
        panel3.add(undeliveredTodayTextField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Потрачено соксов за СЕГОДНЯ");
        panel3.add(label6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        usedProxyTodayTextField = new JTextField();
        usedProxyTodayTextField.setEditable(false);
        panel3.add(usedProxyTodayTextField, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Получено соксов за СЕГОДНЯ");
        panel3.add(label7, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        countDownloadedProxyTodayTextField = new JTextField();
        countDownloadedProxyTodayTextField.setEditable(false);
        panel3.add(countDownloadedProxyTodayTextField, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel4, new GridConstraints(0, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel4.setBorder(BorderFactory.createTitledBorder("Статистика по категориям"));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel4.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 150), new Dimension(-1, 150), new Dimension(-1, 150), 0, false));
        categoryTable = new JXTable();
        categoryTable.setFillsViewportHeight(false);
        categoryTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        scrollPane1.setViewportView(categoryTable);
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel5, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Живые соксы");
        panel5.add(label8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        socksComboBox = new JComboBox();
        socksComboBox.setEnabled(true);
        panel5.add(socksComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, new Dimension(350, -1), new Dimension(350, -1), new Dimension(350, -1), 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Процесс отправки ответов");
        panel5.add(label9, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        messageSenderProgressBar = new JProgressBar();
        panel5.add(messageSenderProgressBar, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel6.setBorder(BorderFactory.createTitledBorder("Выгрузка отправленных писем"));
        sentMessages1DateTimePicker = new BfDateTimePicker();
        panel6.add(sentMessages1DateTimePicker, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        sentMessages2DateTimePicker = new BfDateTimePicker();
        panel6.add(sentMessages2DateTimePicker, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("до");
        panel6.add(label10, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("От");
        panel6.add(label11, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel6.add(spacer2, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        saveSentMessagesButton = new JButton();
        saveSentMessagesButton.setText("Сохранить");
        panel6.add(saveSentMessagesButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel1.add(spacer3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(4, 1, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("Настройки", panel7);
        final Spacer spacer4 = new Spacer();
        panel7.add(spacer4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel8, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel8.setBorder(BorderFactory.createTitledBorder("Доступ к http://admin.5socks.net/"));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(7, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("Логин");
        panel9.add(label12, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        proxyPanelUserTextField = new JTextField();
        panel9.add(proxyPanelUserTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        final JLabel label13 = new JLabel();
        label13.setText("Пароль");
        panel9.add(label13, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        proxyPanelPassField = new JPasswordField();
        panel9.add(proxyPanelPassField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        final JLabel label14 = new JLabel();
        label14.setText("Время жизни прокси");
        panel9.add(label14, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        proxyTtlSpinner = new JSpinner();
        panel9.add(proxyTtlSpinner, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        final JLabel label15 = new JLabel();
        label15.setText("Максимальное количество прокси за СЕГОДНЯ");
        panel9.add(label15, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maxCountProxyTodaySpinner = new JSpinner();
        panel9.add(maxCountProxyTodaySpinner, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        final JLabel label16 = new JLabel();
        label16.setText("Максимальное число ошибок на один прокси");
        panel9.add(label16, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        proxyMaxErrorCountSpinner = new JSpinner();
        panel9.add(proxyMaxErrorCountSpinner, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        final JLabel label17 = new JLabel();
        label17.setText("Количество прокси для отправки уведомления");
        panel9.add(label17, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel9.add(spacer5, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        socksThresholdSpinner = new JSpinner();
        panel9.add(socksThresholdSpinner, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel10, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel10.add(scrollPane2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        proxyPanelUrlsTextArea = new JTextArea();
        scrollPane2.setViewportView(proxyPanelUrlsTextArea);
        final JLabel label18 = new JLabel();
        label18.setText("Ссылки на списки прокси");
        panel10.add(label18, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel11, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel11.setBorder(BorderFactory.createTitledBorder("Общие настройки"));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel11.add(panel12, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label19 = new JLabel();
        label19.setText("Шаблон письма");
        panel12.add(label19, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane3 = new JScrollPane();
        panel12.add(scrollPane3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 150), new Dimension(-1, 150), new Dimension(-1, 200), 0, false));
        messageTemplateTextArea = new JTextArea();
        scrollPane3.setViewportView(messageTemplateTextArea);
        final JTabbedPane tabbedPane4 = new JTabbedPane();
        panel11.add(tabbedPane4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(6, 2, new Insets(3, 3, 3, 3), -1, -1));
        tabbedPane4.addTab("Время и интервалы", panel13);
        final JLabel label20 = new JLabel();
        label20.setText("Установить значение СЕГОДНЯ");
        panel13.add(label20, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        panel13.add(spacer6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        todayClockComboBox = new JComboBox();
        panel13.add(todayClockComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label21 = new JLabel();
        label21.setText("Интервал проверки входящих");
        panel13.add(label21, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fetchEmailIntervalSpinner = new JSpinner();
        panel13.add(fetchEmailIntervalSpinner, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label22 = new JLabel();
        label22.setText("Задержка перед отправкой ответа");
        panel13.add(label22, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sendingDelaySpinner = new JSpinner();
        panel13.add(sendingDelaySpinner, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label23 = new JLabel();
        label23.setText("Интервал между отправками писем");
        panel13.add(label23, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        timeoutBetweenMessageSendSpinner = new JSpinner();
        panel13.add(timeoutBetweenMessageSendSpinner, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label24 = new JLabel();
        label24.setText("Количество скачиваемых за один раз писем");
        panel13.add(label24, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        onceMessageCountSpinner = new JSpinner();
        panel13.add(onceMessageCountSpinner, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new GridLayoutManager(4, 2, new Insets(3, 3, 3, 3), -1, -1));
        tabbedPane4.addTab("Поиск email", panel14);
        disableEmailSearchInFromCheckBox = new JCheckBox();
        disableEmailSearchInFromCheckBox.setText("Не искать email в поле From");
        panel14.add(disableEmailSearchInFromCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer7 = new Spacer();
        panel14.add(spacer7, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        disableEmailSearchInBodyCheckBox = new JCheckBox();
        disableEmailSearchInBodyCheckBox.setText("Не искать email в теле письма");
        panel14.add(disableEmailSearchInBodyCheckBox, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        disableEmailSearchInAttachmentsCheckBox = new JCheckBox();
        disableEmailSearchInAttachmentsCheckBox.setText("Не искать email во вложениях");
        panel14.add(disableEmailSearchInAttachmentsCheckBox, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new GridLayoutManager(5, 2, new Insets(3, 3, 3, 3), -1, -1));
        tabbedPane4.addTab("Заголовки писем", panel15);
        final JLabel label25 = new JLabel();
        label25.setText("Email администратора");
        panel15.add(label25, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer8 = new Spacer();
        panel15.add(spacer8, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        adminEmailTextField = new JTextField();
        panel15.add(adminEmailTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label26 = new JLabel();
        label26.setText("Имя отправителя");
        panel15.add(label26, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        senderNameTextField = new JTextField();
        panel15.add(senderNameTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label27 = new JLabel();
        label27.setText("Имя для обратного адреса");
        panel15.add(label27, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        replyToTextField = new JTextField();
        panel15.add(replyToTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label28 = new JLabel();
        label28.setText("Тема письма повторной рассылки");
        panel15.add(label28, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        repeatedSubjectTextField = new JTextField();
        panel15.add(repeatedSubjectTextField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        tabbedPane3 = new JTabbedPane();
        panel7.add(tabbedPane3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel16 = new JPanel();
        panel16.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane3.addTab("Почта для получения", panel16);
        final JPanel panel17 = new JPanel();
        panel17.setLayout(new GridLayoutManager(2, 2, new Insets(5, 5, 5, 5), -1, -1));
        panel16.add(panel17, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label29 = new JLabel();
        label29.setText("Email");
        panel17.add(label29, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label30 = new JLabel();
        label30.setText("Пароль");
        panel17.add(label30, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pop3EmailLoginTextField = new JTextField();
        panel17.add(pop3EmailLoginTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        pop3EmailPasswordField = new JPasswordField();
        panel17.add(pop3EmailPasswordField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel18 = new JPanel();
        panel18.setLayout(new GridLayoutManager(2, 2, new Insets(5, 5, 5, 5), -1, -1));
        panel16.add(panel18, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label31 = new JLabel();
        label31.setText("POP3 сервер");
        panel18.add(label31, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label32 = new JLabel();
        label32.setText("POP3 порт");
        panel18.add(label32, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pop3HostTextField = new JTextField();
        panel18.add(pop3HostTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        pop3PortSpinner = new JSpinner();
        panel18.add(pop3PortSpinner, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel19 = new JPanel();
        panel19.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel16.add(panel19, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label33 = new JLabel();
        label33.setText("Таймаут ответа почтового сервера");
        panel19.add(label33, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pop3MailboxConnectionTimeoutSpinner = new JSpinner();
        panel19.add(pop3MailboxConnectionTimeoutSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer9 = new Spacer();
        panel16.add(spacer9, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel20 = new JPanel();
        panel20.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane3.addTab("Почта для отправки", panel20);
        final JPanel panel21 = new JPanel();
        panel21.setLayout(new GridLayoutManager(3, 2, new Insets(5, 5, 5, 5), -1, -1));
        panel20.add(panel21, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label34 = new JLabel();
        label34.setText("Email");
        panel21.add(label34, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label35 = new JLabel();
        label35.setText("Пароль");
        panel21.add(label35, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        smtpEmailLoginTextField = new JTextField();
        panel21.add(smtpEmailLoginTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        smtpEmailPasswordField = new JPasswordField();
        panel21.add(smtpEmailPasswordField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer10 = new Spacer();
        panel21.add(spacer10, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer11 = new Spacer();
        panel20.add(spacer11, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel22 = new JPanel();
        panel22.setLayout(new GridLayoutManager(2, 3, new Insets(5, 5, 5, 5), -1, -1));
        panel20.add(panel22, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label36 = new JLabel();
        label36.setText("SMTP сервер");
        panel22.add(label36, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label37 = new JLabel();
        label37.setText("SMTP порт");
        panel22.add(label37, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        smtpHostTextField = new JTextField();
        panel22.add(smtpHostTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        smtpPortSpinner = new JSpinner();
        panel22.add(smtpPortSpinner, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        smtpSslCheckBox = new JCheckBox();
        smtpSslCheckBox.setText("Шифрование");
        panel22.add(smtpSslCheckBox, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel23 = new JPanel();
        panel23.setLayout(new GridLayoutManager(2, 2, new Insets(5, 5, 5, 5), -1, -1));
        panel20.add(panel23, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label38 = new JLabel();
        label38.setText("Таймаут ответа почтового сервера");
        panel23.add(label38, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        smtpMailboxConnectionTimeoutSpinner = new JSpinner();
        panel23.add(smtpMailboxConnectionTimeoutSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label39 = new JLabel();
        label39.setText("Поле FROM");
        panel23.add(label39, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        senderFromTextField = new JTextField();
        panel23.add(senderFromTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel24 = new JPanel();
        panel24.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Другие настройки", panel24);
        final JPanel panel25 = new JPanel();
        panel25.setLayout(new GridLayoutManager(4, 8, new Insets(0, 0, 0, 0), -1, -1));
        panel24.add(panel25, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel25.setBorder(BorderFactory.createTitledBorder("Расписание и скорость"));
        final JLabel label40 = new JLabel();
        label40.setText("Понедельник");
        panel25.add(label40, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(80, -1), new Dimension(80, -1), new Dimension(80, -1), 0, false));
        monday1ComboBox = new JComboBox();
        panel25.add(monday1ComboBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        monday2ComboBox = new JComboBox();
        panel25.add(monday2ComboBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label41 = new JLabel();
        label41.setText("Вторник");
        panel25.add(label41, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(80, -1), new Dimension(80, -1), new Dimension(80, -1), 0, false));
        tuesday1ComboBox = new JComboBox();
        panel25.add(tuesday1ComboBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tuesday2ComboBox = new JComboBox();
        panel25.add(tuesday2ComboBox, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label42 = new JLabel();
        label42.setText("Среда");
        panel25.add(label42, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(80, -1), new Dimension(80, -1), new Dimension(80, -1), 0, false));
        wednesday1ComboBox = new JComboBox();
        panel25.add(wednesday1ComboBox, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        wednesday2ComboBox = new JComboBox();
        panel25.add(wednesday2ComboBox, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label43 = new JLabel();
        label43.setText("Четверг");
        panel25.add(label43, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(80, -1), new Dimension(80, -1), new Dimension(80, -1), 0, false));
        thursday1ComboBox = new JComboBox();
        panel25.add(thursday1ComboBox, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thursday2ComboBox = new JComboBox();
        panel25.add(thursday2ComboBox, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label44 = new JLabel();
        label44.setText("Пятница");
        panel25.add(label44, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(80, -1), new Dimension(80, -1), new Dimension(80, -1), 0, false));
        friday1ComboBox = new JComboBox();
        panel25.add(friday1ComboBox, new GridConstraints(2, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        friday2ComboBox = new JComboBox();
        panel25.add(friday2ComboBox, new GridConstraints(3, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label45 = new JLabel();
        label45.setText("Суббота");
        panel25.add(label45, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(80, -1), new Dimension(80, -1), new Dimension(80, -1), 0, false));
        saturday1ComboBox = new JComboBox();
        panel25.add(saturday1ComboBox, new GridConstraints(2, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saturday2ComboBox = new JComboBox();
        panel25.add(saturday2ComboBox, new GridConstraints(3, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label46 = new JLabel();
        label46.setText("Воскресенье");
        panel25.add(label46, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(80, -1), new Dimension(80, -1), new Dimension(80, -1), 0, false));
        sunday1ComboBox = new JComboBox();
        panel25.add(sunday1ComboBox, new GridConstraints(2, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sunday2ComboBox = new JComboBox();
        panel25.add(sunday2ComboBox, new GridConstraints(3, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mondayCheckBox = new JCheckBox();
        mondayCheckBox.setText("Весь день");
        panel25.add(mondayCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tuesdayCheckBox = new JCheckBox();
        tuesdayCheckBox.setText("Весь день");
        panel25.add(tuesdayCheckBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        wednesdayCheckBox = new JCheckBox();
        wednesdayCheckBox.setText("Весь день");
        panel25.add(wednesdayCheckBox, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thursdayCheckBox = new JCheckBox();
        thursdayCheckBox.setText("Весь день");
        panel25.add(thursdayCheckBox, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fridayCheckBox = new JCheckBox();
        fridayCheckBox.setText("Весь день");
        panel25.add(fridayCheckBox, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saturdayCheckBox = new JCheckBox();
        saturdayCheckBox.setText("Весь день");
        panel25.add(saturdayCheckBox, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sundayCheckBox = new JCheckBox();
        sundayCheckBox.setText("Весь день");
        panel25.add(sundayCheckBox, new GridConstraints(1, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel26 = new JPanel();
        panel26.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel25.add(panel26, new GridConstraints(0, 7, 4, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel27 = new JPanel();
        panel27.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel26.add(panel27, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        speedSlider = new JSlider();
        speedSlider.setInverted(true);
        speedSlider.setMajorTickSpacing(0);
        speedSlider.setMaximum(2000);
        speedSlider.setMinimum(0);
        speedSlider.setPaintLabels(true);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintTrack(true);
        speedSlider.setSnapToTicks(true);
        speedSlider.setValue(10);
        panel27.add(speedSlider, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label47 = new JLabel();
        label47.setText("Скорость");
        panel27.add(label47, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer12 = new Spacer();
        panel26.add(spacer12, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel28 = new JPanel();
        panel28.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel26.add(panel28, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label48 = new JLabel();
        label48.setText("Локализация");
        panel28.add(label48, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer13 = new Spacer();
        panel28.add(spacer13, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        localePreviewLabel = new JLabel();
        localePreviewLabel.setText("localePreviewLabel");
        panel28.add(localePreviewLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        localeComboBox = new JComboBox();
        panel28.add(localeComboBox, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer14 = new Spacer();
        panel24.add(spacer14, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel29 = new JPanel();
        panel29.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel24.add(panel29, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel29.setBorder(BorderFactory.createTitledBorder("Изменить пароль"));
        final JLabel label49 = new JLabel();
        label49.setText("Новый пароль");
        panel29.add(label49, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        keyTextField = new JTextField();
        panel29.add(keyTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        saveKeyButton = new JButton();
        saveKeyButton.setText("Применить");
        panel29.add(saveKeyButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel30 = new JPanel();
        panel30.setLayout(new GridLayoutManager(3, 1, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("Фильтры", panel30);
        final JPanel panel31 = new JPanel();
        panel31.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel30.add(panel31, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel31.setBorder(BorderFactory.createTitledBorder("Обработка вложений"));
        final JPanel panel32 = new JPanel();
        panel32.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel31.add(panel32, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label50 = new JLabel();
        label50.setText("Теги в именах файлов вложений для поиска обратного адреса");
        panel32.add(label50, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane4 = new JScrollPane();
        panel32.add(scrollPane4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 100), new Dimension(-1, 100), new Dimension(-1, 100), 0, false));
        emailForResponseFinderFilenameTagsTextArea = new JTextArea();
        scrollPane4.setViewportView(emailForResponseFinderFilenameTagsTextArea);
        final JPanel panel33 = new JPanel();
        panel33.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel31.add(panel33, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label51 = new JLabel();
        label51.setText("Порядок обработки вложений");
        panel33.add(label51, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane5 = new JScrollPane();
        panel33.add(scrollPane5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(170, 100), new Dimension(170, 100), new Dimension(170, 100), 0, false));
        emailForResponseFinderExtensionsList = new JList();
        scrollPane5.setViewportView(emailForResponseFinderExtensionsList);
        final JPanel panel34 = new JPanel();
        panel34.setLayout(new GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel30.add(panel34, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        panel34.setBorder(BorderFactory.createTitledBorder("Категории писем"));
        final JScrollPane scrollPane6 = new JScrollPane();
        panel34.add(scrollPane6, new GridConstraints(0, 0, 5, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        folderList = new JList();
        scrollPane6.setViewportView(folderList);
        folderChangeButton = new JButton();
        folderChangeButton.setText("Изменить");
        panel34.add(folderChangeButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        folderDeleteButton = new JButton();
        folderDeleteButton.setText("Удалить");
        panel34.add(folderDeleteButton, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        folderAddButton = new JButton();
        folderAddButton.setText("Добавить");
        panel34.add(folderAddButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        setDefaultFolderButton = new JButton();
        setDefaultFolderButton.setText("По умолчанию");
        panel34.add(setDefaultFolderButton, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer15 = new Spacer();
        panel34.add(spacer15, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tabbedPane2 = new JTabbedPane();
        panel30.add(tabbedPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 180), new Dimension(200, 180), new Dimension(-1, 180), 0, false));
        final JPanel panel35 = new JPanel();
        panel35.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane2.addTab("Фильтр по времени последнего ответа на этот же адрес", panel35);
        minResponseIntervalPanel = new JPanel();
        minResponseIntervalPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel35.add(minResponseIntervalPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        minResponseIntervalPanel.setBorder(BorderFactory.createTitledBorder("Фильтр по времени последнего ответа на этот же адрес"));
        final JLabel label52 = new JLabel();
        label52.setText("Минимальное время между письмами одному и тому же адресату");
        minResponseIntervalPanel.add(label52, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minResponseIntervalSpinner = new JSpinner();
        minResponseIntervalPanel.add(minResponseIntervalSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel36 = new JPanel();
        panel36.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        minResponseIntervalPanel.add(panel36, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label53 = new JLabel();
        label53.setText("Проверяем первые");
        panel36.add(label53, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minResponseIntervalFirstSpinner = new JSpinner();
        panel36.add(minResponseIntervalFirstSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(30, -1), new Dimension(30, -1), new Dimension(30, -1), 0, false));
        final JLabel label54 = new JLabel();
        label54.setText("символов адреса, если одинаковые, то удаляем");
        panel36.add(label54, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minResponseIntervalLastSpinner = new JSpinner();
        panel36.add(minResponseIntervalLastSpinner, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(30, -1), new Dimension(30, -1), new Dimension(30, -1), 0, false));
        final JLabel label55 = new JLabel();
        label55.setText("символов перед @: если остатки одинаковые, то не отправлять");
        panel36.add(label55, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer16 = new Spacer();
        panel35.add(spacer16, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        responseTimeFilterCheckBox = new JCheckBox();
        responseTimeFilterCheckBox.setText("");
        panel35.add(responseTimeFilterCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel37 = new JPanel();
        panel37.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane2.addTab("Фильтр удаления по теме и полю From", panel37);
        deleteBySubjectAndFromFilterPanel = new JPanel();
        deleteBySubjectAndFromFilterPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel37.add(deleteBySubjectAndFromFilterPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        deleteBySubjectAndFromFilterPanel.setBorder(BorderFactory.createTitledBorder("Фильтр удаления по теме и полю From"));
        final JScrollPane scrollPane7 = new JScrollPane();
        deleteBySubjectAndFromFilterPanel.add(scrollPane7, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 100), new Dimension(-1, 100), new Dimension(-1, 100), 0, false));
        deleteBySubjectAndFromFilterTagsTextArea = new JTextArea();
        scrollPane7.setViewportView(deleteBySubjectAndFromFilterTagsTextArea);
        final JLabel label56 = new JLabel();
        label56.setText("Теги по одному на строку");
        deleteBySubjectAndFromFilterPanel.add(label56, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer17 = new Spacer();
        panel37.add(spacer17, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        deleteBySubjectAndFromFilterCheckBox = new JCheckBox();
        deleteBySubjectAndFromFilterCheckBox.setText("");
        panel37.add(deleteBySubjectAndFromFilterCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel38 = new JPanel();
        panel38.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane2.addTab("Фильтр неудаления по теме и полю From", panel38);
        antiDeleteBySubjectAndFromFilterPanel = new JPanel();
        antiDeleteBySubjectAndFromFilterPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel38.add(antiDeleteBySubjectAndFromFilterPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        antiDeleteBySubjectAndFromFilterPanel.setBorder(BorderFactory.createTitledBorder("Фильтр неудаления по теме и полю From"));
        final JScrollPane scrollPane8 = new JScrollPane();
        antiDeleteBySubjectAndFromFilterPanel.add(scrollPane8, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 100), new Dimension(-1, 100), new Dimension(-1, 100), 0, false));
        antiDeleteBySubjectAndFromFilterTagsTextArea = new JTextArea();
        scrollPane8.setViewportView(antiDeleteBySubjectAndFromFilterTagsTextArea);
        final JLabel label57 = new JLabel();
        label57.setText("Теги по одному на строку");
        antiDeleteBySubjectAndFromFilterPanel.add(label57, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer18 = new Spacer();
        panel38.add(spacer18, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        antiDeleteBySubjectAndFromFilterCheckBox = new JCheckBox();
        antiDeleteBySubjectAndFromFilterCheckBox.setText("");
        panel38.add(antiDeleteBySubjectAndFromFilterCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel39 = new JPanel();
        panel39.setLayout(new GridLayoutManager(2, 1, new Insets(5, 5, 5, 5), -1, -1));
        tabbedPane1.addTab("Лог", panel39);
        logScrollPane = new JScrollPane();
        panel39.add(logScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        logTextArea = new JTextArea();
        logScrollPane.setViewportView(logTextArea);
        logFontChooserComboBox = new FontChooserComboBox();
        logFontChooserComboBox.setPreviewString("Привет, мир!");
        panel39.add(logFontChooserComboBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        actionButton = new JButton();
        actionButton.setText("Старт");
        rootPanel.add(actionButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threadControlButton = new JButton();
        threadControlButton.setEnabled(false);
        threadControlButton.setText("Пауза");
        rootPanel.add(threadControlButton, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer19 = new Spacer();
        rootPanel.add(spacer19, new GridConstraints(2, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label58 = new JLabel();
        label58.setText("Загрузка процессора");
        rootPanel.add(label58, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cpuLoadProgressBar = new JProgressBar();
        rootPanel.add(cpuLoadProgressBar, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        yellowPanel = new JPanel();
        yellowPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(yellowPanel, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(24, 24), new Dimension(24, 24), new Dimension(24, 24), 0, false));
        yellowLabel = new JLabel();
        yellowLabel.setText("");
        yellowLabel.setToolTipText("Идёт получение писем");
        yellowPanel.add(yellowLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        greenPanel = new JPanel();
        greenPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(greenPanel, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(24, 24), new Dimension(24, 24), new Dimension(24, 24), 0, false));
        greenLabel = new JLabel();
        greenLabel.setText("");
        greenLabel.setToolTipText("Идёт отправка писем");
        greenPanel.add(greenLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }
}

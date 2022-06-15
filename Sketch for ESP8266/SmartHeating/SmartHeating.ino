#include <ESP8266WiFi.h> // библиотека для работы с WiFi на плате ESP8266
#include <ESP8266WebServer.h> // библиотека для работы с Web сервером на плате ESP8266
#include <Firebase_ESP_Client.h> // библиотека для работы с Firebase на плате ESP8266

// вспомогательные библиотеки для работы с Firebase
#include <addons/TokenHelper.h>
#include <addons/RTDBHelper.h>

#include <NTPClient.h> // библиотека для получения времени от NTP сервера
#include <WiFiUdp.h> // библиотека для работы с UDP клиентом

// библиотеки для работы с файловой системой SPIFFS
#include <FS.h>
#include <LittleFS.h>

#include "WebPage.h" // Web страница для настройки параметров работы

#define TIMEZONE 3 // ваш часовой пояс

#define FIRST_HEATING_ELEMENT_RELAY_PIN 15  // пин к которому подключено реле для включения 1 тэна котла
#define SECOND_HEATING_ELEMENT_RELAY_PIN 14 // пин к которому подключено реле для включения 2 тэнов котла
#define THIRD_HEATING_ELEMENT_RELAY_PIN 5   // пин к которому подключено реле для включения 3 тэнов котла

#define HEATING_PUMP_RELAY_PIN 12 // пин к которому подключено реле для включения насоса
#define BOILER_RELAY_PIN 13 // пин к которому подключено реле для включения бойлера
#define RESET_BUTTON_PIN 4 // пин к которому подключена кнопка для перехода в режим настройки параметров работы

String ssidName = "";  // переменная для названия вашей WiFi сети
String ssidPass = "";  // переменная для пароля вашей WiFi сети
String userEmail = ""; // переменная для почты вашего пользователя
String userPass = "";  // переменная для пароля вашего пользователя

bool setSettingsMode = false; // переменная для хранения того, запущен ли режим настройки параметров работы
bool streamStartedNow = false;

int maxHeatingElements = 0; // переменная для количества тэнов вашего котла
int heatingElements = 0; // переменная для хранения того, сколько сейчас запущено тэнов котла

bool heatingTimeModeStarted = false; // переменная для хранения того, запущен ли режим включения и выключения котла по времени
String heatingTimestamps[16]; // массив для времени включения и выключения котла
int heatingTimestampTypes[16]; // массив для алгоритма включения и выключения котла
int heatingElementsArray[16]; // массив для алгоритма включения тэнов котла
int heatingTimestampsCount = 0; // переменная для количества отметок времени включения и выключения котла

bool boilerTimeModeStarted = false; // переменная для хранения того, запущен ли режим включения и выключения бойлера по времени
String boilerTimestamps[16]; // массив для хранения времени включения и выключения бойлера
int boilerTimestampTypes[16]; // массив для хранения алгоритма включения и выключения бойлера
int boilerTimestampsCount = 0; // переменная для количества отметок времени включения и выключения бойлера

bool temperatureModeStarted = false; // переменная для хранения того, запущен ли режим управления котлом по температуре
int startHeatingTemperature = 0; // переменная для минимальной температуры в установленном вами диапазоне
int stopHeatingTemperature = 0; // переменная для максимальной температуры в установленном вами диапазоне
bool heatingPhase = false; // переменная для фазы работы котла в режиме управления котлом по температуре

unsigned long heatingTimerMillis = 0; // переменная для millis таймера котла
unsigned long boilerTimerMillis = 0; // переменная для millis таймера бойлера
int heatingTimerSeconds = 0; // переменная для времени с запуска таймера котла
int heatingTimerTime = 0; // переменная для установленного вами времени работы таймера котла
int boilerTimerSeconds = 0; // переменная для времени с запуска таймера бойлера
int boilerTimerTime = 0; // переменная для установленного вами времени работы таймера бойлера

unsigned long getTimeMillis = 0; // переменная для времени получения времени от NTP сервера в millis
unsigned long resetButtonMillis = 0; // переменная для времени нажатия кнопки для перехода в режим настройки параметров в millis
unsigned long WiFiDisconnectedMillis = 0;
bool resetButtonFlag = false; // переменная для флажка кнопки

FirebaseData firebaseStream; // слушатель изменения данных узла вашего пользователя в Firebase
FirebaseData firebaseData; // объект для работы с данными из базы данных Firebase
FirebaseAuth firebaseAuth; // объект для авторизации в Firebase
FirebaseConfig firebaseConfig; // объект для настройки подключения к базе данных firebase

WiFiUDP ntpUDP; // WiFiUDP клиент
NTPClient timeClient(ntpUDP, "pool.ntp.org", TIMEZONE * 3600, 10000); // настраиваем NTP клиент

ESP8266WebServer server(80); // объект для управления Web сервером

// функция, которая срабатывает при изменении данных узла вашего пользователя в Firebase, или же, проще говоря, при получении какой-либо команды
void firebaseStreamCallback(MultiPathStream streamData) {
  if (WiFi.status() == WL_CONNECTED && !streamStartedNow) {
    if (streamData.get("/heatingElements")) { // если получено количество тэнов котла, которое нужно включить
      heatingElements = streamData.value.toInt(); // записываем данные узла для хранения установленного вами количества тэнов котла
    }

    if (streamData.get("/heatingArray")) { // если получен массив с алгоритмом включения тэнов котла
      String heatingElementsArrayString = String(streamData.value.c_str()); // записываем данные узла для хранения массива с алгоритмом включения тэнов котла
      int arrayItems = 0; // переменная для количества элементов массива с алгоритмом включения тэнов котла
      for (int index = 1; index < heatingElementsArrayString.length() - 1;) {
        heatingElementsArray[arrayItems] = (String(heatingElementsArrayString.charAt(index))).toInt(); // добавляем в массив количество тэнов, которое нужно включить в определённое время
        index = index + 2;
        arrayItems++;
      }
    }

    if (streamData.get("/heatingStarted")) { // если получена команда на запуск, или остановку котла
      if ((String(streamData.value.c_str())).indexOf("true") != -1) { // если получена команда на запуск котла
        startHeating(heatingElements); // включаем установленное вами количество тэнов
        Serial.println("Котёл запущен! Количество тэнов: " + String(heatingElements));
      } else { // если получена команда на остановку котла
        stopHeating(); // останавливаем котёл
        Serial.println("Котёл остановлен!");
      }
    }

    if (streamData.get("/boilerStarted")) { // если получена команда на запуск, или остановку бойлера
      if ((String(streamData.value.c_str())).indexOf("true") != -1) { // если получена команда на запуск бойлера
        digitalWrite(BOILER_RELAY_PIN, HIGH); // замыкаем реле к которому подключен бойлер
        Serial.println("Бойлер запущен!");
      } else { // если получена команда на остановку бойлера
        digitalWrite(BOILER_RELAY_PIN, LOW); // размыкаем реле к которому подключен бойлер
        Serial.println("Бойлер остановлен!");
      }
    }

    if (streamData.get("/heatingTimerTime")) { // если получено время работы таймера котла
      heatingTimerTime = streamData.value.toInt(); // записываем данные узла для хранения времени работы таймера котла
      if (heatingTimerTime > 0) { // если получена команда на запуск таймера котла
        startHeating(heatingElements); // включаем установленное вами количество тэнов
        Serial.println("Таймер котла запущен!\nКоличество тэнов: " + String(heatingElements) + "\nВремя работы таймера: " + String(heatingTimerTime));
      } else {
        stopHeating(); // останавливаем котёл
        heatingTimerTime = 0;
        heatingTimerSeconds = 0;
        heatingTimerMillis = 0;
        Serial.println("Таймер котла остановлен!");
      }
    }

    if (streamData.get("/boilerTimerTime")) { // если получено время работы таймера бойлера
      boilerTimerTime = streamData.value.toInt(); // записываем данные узла для хранения времени работы таймера бойлера
      if (boilerTimerTime > 0) { // если получена команда на запуск таймера бойлера
        digitalWrite(BOILER_RELAY_PIN, HIGH); // замыкаем реле к которому подключен бойлер
        Serial.println("Таймер бойлера запущен! Время работы таймера: " + String(boilerTimerTime));
      } else {
        digitalWrite(BOILER_RELAY_PIN, LOW); // размыкаем реле к которому подключен бойлер
        boilerTimerTime = 0;
        Serial.println("Таймер бойлера остановлен!");
      }
    }

    if (streamData.get("/heatingTime")) { // если получен массив с временем включения и выключения котла
      String heatingTimestampsString = String(streamData.value.c_str()); // записываем данные узла для хранения массива с временем включения и выключения котла
      if (heatingTimestampsString != "0") { // если получена команда на запуск режима включения и выключения котла по времени
        heatingTimeModeStarted = true; // записываем то, что режим включения и выключения котла по времени запущен
        bool heatingTimestampType; // переменная для хранения того, нужно ли включить, или выключить котёл в определённое время

        if (!boilerTimeModeStarted) timeClient.begin(); // если режим включения и выключения бойлера по времени не запущен, запускаем NTP клиент

        for (int index = 2; index < heatingTimestampsString.length() - 1;) { // цикл для превращения строки в массив
          heatingTimestamps[heatingTimestampsCount] = heatingTimestampsString.substring(index, index + 5); // выделяем время из строки с массивом
          heatingTimestampType = !heatingTimestampType; // записываем то, нужно ли в это время включить, или выключить котёл
          if (heatingTimestampType) { // если в это время нужно включить котёл
            heatingTimestampTypes[heatingTimestampsCount] = 1; // добавляем в массив с алгоритмом включения котла то, что в это время нужно включить котёл
          } else {
            heatingTimestampTypes[heatingTimestampsCount] = 0; // добавляем в массив с алгоритмом включения котла то, что в это время нужно выключить котёл
          }
          Serial.println(heatingTimestampsString.substring(index, index + 5));
          heatingTimestampsCount++;
          index = index + 8;
        }
        Serial.println("Режим работы котла по времени запущен!");
      } else {
        stopHeating(); // останавливаем котёл
        heatingTimeModeStarted = false; // записываем то, что режим включения и выключения котла по времени остановлен
        heatingTimestampsCount = 0;
        Serial.println("Режим работы котла по времени остановлен!");
      }
    }

    if (streamData.get("/boilerTime")) { // если получен массив с временем включения и выключения бойлера
      String boilerTimestampsString = String(streamData.value.c_str()); // записываем данные узла для хранения массива с временем включения и выключения бойлера
      if (boilerTimestampsString != "0") { // если получена команда на запуск режима включения и выключения бойлера по времени
        boilerTimeModeStarted = true; // записываем то, что режим включения и выключения бойлера по времени запущен
        bool boilerTimestampType; // переменная для хранения того, нужно ли включить, или выключить бойлер в определённое время

        if (!heatingTimeModeStarted) timeClient.begin(); // если режим включения и выключения котла по времени не запущен, запускаем NTP клиент

        for (int index = 2; index < boilerTimestampsString.length() - 1;) { // цикл для превращения строки в массив
          boilerTimestamps[boilerTimestampsCount] = boilerTimestampsString.substring(index, index + 5); // выделяем время из строки с массивом
          boilerTimestampType = !boilerTimestampType; // записываем то, нужно ли в это время включить, или выключить бойлер
          if (boilerTimestampType) { // если в это время нужно включить бойлер
            boilerTimestampTypes[boilerTimestampsCount] = 1; // добавляем в массив с алгоритмом включения бойлера то, что в это время нужно включить бойлер
          } else {
            heatingTimestampTypes[boilerTimestampsCount] = 0; // добавляем в массив с алгоритмом включения бойлера то, что в это время нужно выключить бойлер
          }
          Serial.println(boilerTimestampsString.substring(index, index + 5));
          boilerTimestampsCount++;
          index = index + 8;
        }
        Serial.println("Режим работы бойлера по времени запущен!");
      } else {
        digitalWrite(BOILER_RELAY_PIN, LOW); // размыкаем реле к которому подключен бойлер
        boilerTimeModeStarted = false; // записываем то, что режим включения и выключения бойлера по времени остановлен
        boilerTimestampsCount = 0;
        Serial.println("Режим работы бойлера по времени остановлен!");
      }
    }

    if (streamData.get("/temperatureRange")) { // если получен диапазон температуры, которую нужно поддерживать в помещении
      String temperatureRange = String(streamData.value.c_str()); // записываем данные узла для хранения диапазона температуры, которую нужно поддерживать в помещении
      if (temperatureRange != "0") { // если получена команда на запуск режима управления котлом по температуре
        temperatureModeStarted = true; // записываем то, что режим управления котлом по температуре запущен
        startHeatingTemperature = (temperatureRange.substring(0, temperatureRange.indexOf(" "))).toInt(); // записываем минимальную температуру диапазоне температуры
        stopHeatingTemperature = (temperatureRange.substring(temperatureRange.indexOf(" ") + 1, temperatureRange.length())).toInt(); // записываем минимальную температуру диапазоне
        Serial.println("Режим работы котла по температуре запущен!\nДиапазон температуры: " + String(startHeatingTemperature) + " - " + String(stopHeatingTemperature) + "°С");
      } else {
        stopHeating(); // останавливаем котёл
        temperatureModeStarted = false; // записываем то, что режим управления котлом по температуре остановлен
        heatingPhase = false;
        Serial.println("Режим работы котла по температуре остановлен!");
      }
    }

    if (streamData.get("/temperature")) { // если получена температура
      int temperature = streamData.value.toInt(); // записываем данные узла для хранения температуры
      Serial.println("Температура: " + String(temperature));
      if (temperatureModeStarted) { // если запущен режима управления котлом по температуре
        if (!heatingPhase) { // если котёл работает в 1 фазе режима управления по температуре
          if (temperature <= stopHeatingTemperature) { // если текущая температура ниже, или равна максимальной температуры в установленном вами диапазоне температуры
            startHeating(heatingElements); // включаем установленное вами количество тэнов
            Serial.println("Котёл запущен по температуре!");
          } else {
            stopHeating(); // останавливаем котёл
            heatingPhase = true; // переходим в 2 фазу режима управления по температуре
            Serial.println("Котёл остановлен по температуре!");
          }
        }
        if (heatingPhase) { // если котёл работает в 2 фазе режима управления по температуре
          if (temperature < startHeatingTemperature) { // если текущая температура ниже минимальной температуры в установленном вами диапазоне температуры
            startHeating(heatingElements); // включаем установленное вами количество тэнов
            heatingPhase = false; // переходим в 1 фазу режима управления по температуре
            Serial.println("Котёл запущен по температуре!");
          }
        }
      }
    }
  }
  streamStartedNow = false;
}

// функция, которая срабатывает при истечении таймаута слушателя изменения данных
void firebaseStreamTimeoutCallback(bool timeout) {
  if (timeout) {
    Serial.println("Таймаут слушателя изменений узла пользователя истёк!");
    streamStartedNow = true;
  }
  if (!firebaseStream.httpConnected()) Serial.printf("Код ошибки: %d, Причина: %s\n", firebaseStream.httpCode(), firebaseStream.errorReason().c_str());
}

// функция, которая срабатывает при получении каких-либо данных от Web клиента
void handleRoot() {
  String maxHeatingElementsString = String(maxHeatingElements);
  if (maxHeatingElements == 0) {
    maxHeatingElementsString = "";
  }

  // отправляем Web клиенту HTML страницу для настройки параметров работы
  server.send(200, "text/html", String(MainWebPage)
  + "<p><input id=\"ssid\" name=\"ssid\" value=\"" + ssidName + "\"required type=\"text\" spellcheck=\"false\" placeholder=\"Название WiFi сети\"></p>"
  + "<p><input id=\"ssid_pass\" name=\"ssid_pass\" value=\"" + String(ssidPass)+ "\" required type=\"password\" spellcheck=\"false\" minlength=\"8\" placeholder=\"Пароль WiFi сети\">"
  + String(ToggleSsidPass)
  + "<p><input id=\"user_email\" name=\"user_email\" value=\"" + String(userEmail) + "\" required type=\"email\" spellcheck=\"false\"  placeholder=\"Почта пользователя\"></p>"
  + "<p><input id=\"user_pass\" name=\"user_pass\" value=\"" + String(userPass) + "\" required type=\"password\" spellcheck=\"false\" minlength=\"6\" title=\"zxcvbn2\""
  + "placeholder=\"Пароль пользователя\">" + String(ToggleUserPass)
  + "<p><input id=\"heating_elements\" name=\"heating_elements\" value=\"" + String(maxHeatingElementsString) + "\" required type=\"number\" spellcheck=\"false\" min=\"1\""
  + "placeholder=\"Количество тэнов\"></p>" + String(Script));

  // если мы получили параметры работы
  if (server.hasArg("ssid") and server.hasArg("ssid_pass") and server.hasArg("user_email") and server.hasArg("user_pass") and server.hasArg("heating_elements")) {
    ssidName = server.arg("ssid"); // записываем название вашей WiFi сети
    ssidPass = server.arg("ssid_pass"); // записываем пароль вашей WiFi сети
    userEmail = server.arg("user_email"); // записываем почту вашего пользователя
    userPass = server.arg("user_pass"); // записываем пароль вашего пользователя
    maxHeatingElements = (server.arg("heating_elements")).toInt(); // записываем количество тэнов вашего котла

    String settings = ssidName + "#" + ssidPass + "#" + userEmail + "#" + userPass + "#" + String(maxHeatingElements); // записываем все параметры работы
    Serial.println(settings);

    File settingsFile = LittleFS.open("/Settings.txt", "w"); // открываем файл с параметрами работы для записи
    if (settingsFile) {
      Serial.println("Файл с параметрами работы успешно открыт для записи!");
    } else {
      Serial.println("Не удалось открыть файл с параметрами работы для записи :(");
      return;
    }

    if (settingsFile.print(settings)) { // если параметры работы успешно записаны в SPIFFS
      Serial.println("Файл был успешно записан!");
      settingsFile.close(); // закрываем файл с параметрами работы
      server.stop(); // останавливаем Web сервер для настройки параметров работы
      WiFi.softAPdisconnect(true); // останавливаем WiFi точку для настройки параметров работы
      LittleFS.end(); // останавливаем файловую систему SPIFFS
      ESP.restart(); // перезагружаем плату ESP12F
    } else {
      Serial.println("Не удалось записать файл :(");
      return;
    }
  }
}

void setup() {
  Serial.begin(115200);

  pinMode(RESET_BUTTON_PIN, INPUT);

  if (!LittleFS.begin()) { // запускаем файловую систему SPIFFS
    Serial.println("Не удалось монтировать SPIFFS :(");
    ESP.restart();
  }

  if (LittleFS.exists("/Settings.txt")) { // если параметры работы настроены
    File settingsFile = LittleFS.open("/Settings.txt", "r"); // открываем файл с параметрами работы для чтения
    if (settingsFile) {
      String settings = "";
      while (settingsFile.available()) settings = settingsFile.readString(); // прочитываем параметры работы
      settingsFile.close(); // закрываем файл с параметрами работы

      Serial.println("Параметры работы: " + String(settings));

      int secondHashIndex = settings.indexOf("#", settings.indexOf("#") + 1); // записываем индекс второго разделительного хештега
      int thirdHashIndex = settings.indexOf("#", secondHashIndex + 1); // записываем индекс третьего разделительного хештега
      int fourthHashIndex = settings.indexOf("#", thirdHashIndex + 1); // записываем индекс четвёртого разделительного хештега

      ssidName = settings.substring(0, settings.indexOf("#")); // записываем название вашей WiFi сети
      ssidPass = settings.substring(settings.indexOf("#") + 1, secondHashIndex); // записываем пароль вашей WiFi сети
      userEmail = settings.substring(secondHashIndex + 1, thirdHashIndex); // записываем почту вашего пользователя
      userPass = settings.substring(thirdHashIndex + 1, fourthHashIndex); // записываем пароль вашего пользователя
      maxHeatingElements = (settings.substring(fourthHashIndex + 1, settings.length())).toInt(); // записываем количество тэнов вашего котла

      Serial.println("Название WiFi сети: " + String(ssidName));
      Serial.println("Пароль WiFi сети: " + String(ssidPass));
      Serial.println("Почта пользователя: " + String(userEmail));
      Serial.println("Пароль пользователя: " + String(userPass));
      Serial.println("Количество тэнов: " + String(maxHeatingElements));

      if (maxHeatingElements == 1) {
        pinMode(FIRST_HEATING_ELEMENT_RELAY_PIN, OUTPUT);
      } else if (maxHeatingElements == 2) {
        pinMode(FIRST_HEATING_ELEMENT_RELAY_PIN, OUTPUT);
        pinMode(SECOND_HEATING_ELEMENT_RELAY_PIN, OUTPUT);
      } else if (maxHeatingElements == 3) {
        pinMode(FIRST_HEATING_ELEMENT_RELAY_PIN, OUTPUT);
        pinMode(SECOND_HEATING_ELEMENT_RELAY_PIN, OUTPUT);
        pinMode(THIRD_HEATING_ELEMENT_RELAY_PIN, OUTPUT);
      }
      pinMode(HEATING_PUMP_RELAY_PIN, OUTPUT);
      pinMode(BOILER_RELAY_PIN, OUTPUT);

      if (!digitalRead(RESET_BUTTON_PIN)) { // если кнопка для перехода в режим настройки параметров работы нажата
        for (int i = 0; i <= 3025; i++) { // ждём 3 секунды
          if (resetSettings()) break; // если кнопка отпущена, выходим из цикла
          delay(1);
        }
      }

      if (!setSettingsMode) {
        Serial.println("Подключаемся к " + String(ssidName) + " ...");
        WiFi.begin(ssidName.c_str(), ssidPass.c_str()); // подключаемся к вашей WiFi сети
        if (WiFi.waitForConnectResult() == WL_CONNECTED) { // если подключение к вашей WiFi сети прошло успешно
          Serial.print("Подключились к WiFi сети! Локальный IP адрес: "); Serial.println(WiFi.localIP());
          WiFi.setAutoReconnect(true);
          WiFi.persistent(true);

          firebaseAuth.user.email = userEmail.c_str(); // устанавливаем почту Firebase пользователя
          firebaseAuth.user.password = userPass.c_str(); // устанавливаем пароль Firebase пользователя

          firebaseConfig.api_key = "AIzaSyAsvOuNEwx5c-3djpkppsf0tpQUa0SmKmc"; // устанавливаем api ключ базы данных Firebase
          firebaseConfig.database_url = "https://boilerautomaticcontrol-default-rtdb.europe-west1.firebasedatabase.app/"; // устанавливаем url базы данных RTDB из платформы Firebase
          firebaseConfig.token_status_callback = tokenStatusCallback;

          Firebase.begin(&firebaseConfig, &firebaseAuth); // подключаемся к базе данных Firebase
          Firebase.reconnectWiFi(true);

          firebaseStream.setBSSLBufferSize(2048, 512); // устанавливаем размер буфера для данных полученных от пользователя через Firebase

          if (Firebase.RTDB.beginMultiPathStream(&firebaseStream, ("/users/" + firebaseAuth.token.uid).c_str())) { // запускаем слушатель изменения данных
            // устанавливаем функцию, которая будет срабатывать при изменении данных в базе данных Firebase
            Firebase.RTDB.setMultiPathStreamCallback(&firebaseStream, firebaseStreamCallback, firebaseStreamTimeoutCallback);
          } else {
            Serial.printf("\nНе удалось установить слушатель текущего режима работы котла, или бойлера, %s\n\n", firebaseStream.errorReason().c_str());
          }
        } else if (WiFi.waitForConnectResult() == WL_CONNECT_FAILED) { // если подключение к вашей WiFi сети не удалось
          Serial.println("Не удалось подключиться к WiFi сети :(");
        }
      }
    } else {
      Serial.println("Не удалось открыть файл с параметрами работы для чтения :(");
      return;
    }
  } else {
    Serial.println("Вы перешли в режим настройки параметров работы!");
    setSettingsMode = true; // записываем то, что режим настройки параметров работы запущен
    WiFi.softAP("Умное Отопление", ""); // запускаем WiFi точку для настройки параметров работы
    server.on("/", handleRoot); // устанавливаем функцию, которая будет срабатывать при получении данных от Web клиента
    server.begin(); // запускаем Web сервер для настройки параметров работы
  }
}

void loop() {
  if (setSettingsMode) { // если режим настройки параметров работы запущен
    server.handleClient(); // прослушиваем данные от Web клиента
  } else {
    if (heatingTimerTime > 0) { // если запущен таймер котла
      if (millis() - heatingTimerMillis >= 3000) { // делаем задержку 3 секунды
        heatingTimerMillis = millis();

        if (heatingTimerSeconds < heatingTimerTime * 60) { // если время с запуска таймера котла меньше установленного вами времени работы таймера котла
          heatingTimerSeconds = heatingTimerSeconds + 3;
          Serial.println("Время работы таймера котла: " + String(heatingTimerSeconds));
        } else {
          stopHeating(); // останавливаем котёл
          heatingTimerTime = 0;
          heatingTimerSeconds = 0;
          heatingTimerMillis = 0;
          Serial.println("Время работы таймера котла истекло!");
        }
      }
    }

    if (boilerTimerTime > 0) { // если запущен таймер бойлера
      if (millis() - boilerTimerMillis >= 3000) { // делаем задержку 3 секунды
        boilerTimerMillis = millis();

        if (boilerTimerSeconds < boilerTimerTime * 60) { // если время с запуска таймера бойлера меньше установленного вами времени работы таймера бойлера
          boilerTimerSeconds = boilerTimerSeconds + 3;
          Serial.println("Время работы таймера бойлера: " + String(boilerTimerSeconds));
        } else {
          digitalWrite(BOILER_RELAY_PIN, LOW); // размыкаем реле к которому подключен бойлер
          boilerTimerTime = 0;
          boilerTimerSeconds = 0;
          boilerTimerMillis = 0;
          Serial.println("Время работы таймера бойлера истекло!");
        }
      }
    }

    // если плата ESP12F подключена к вашей WiFi сети
    if (WiFi.status() == WL_CONNECTED and ((heatingTimeModeStarted or boilerTimeModeStarted) or (heatingTimeModeStarted and boilerTimeModeStarted))) {
      // если запущен режим включения, или выключения котла, или бойлера по времени
      timeClient.update(); // получаем время от NTP сервера

      if (millis() - getTimeMillis >= 10000) { // делаем задержку 5 секунд
        getTimeMillis = millis();

        String hours = String(timeClient.getHours()); // получаем часы
        String minutes = String(timeClient.getMinutes()); // получаем минуты

        if (hours.toInt() < 10) {
          hours = "0" + hours;
        }
        if (minutes.toInt() < 10) {
          minutes = "0" + minutes;
        }

        if (heatingTimeModeStarted) { // если режим включения, или выключения котла запущен
          for (int index = 0; index < heatingTimestampsCount; index++) { // проверяем каждый элемент массива с временем включения, или выключения котла
            if ((hours + ":" + minutes) == heatingTimestamps[index]) { // если текущее время найдёно в массиве
              if (heatingTimestampTypes[index] == 1) { // если в это время нужно включить котёл
                startHeating(heatingElementsArray[index]); // включаем то количество тэнов, которое нужно включить в это время
                Serial.println("Котёл запущен по времени! Количество тэнов: " + String(heatingElementsArray[index])
                               + " Текущее время: " + String(hours) + ":" + String(minutes));
              } else { // если в это время нужно выключить котёл
                stopHeating(); // останавливаем котёл
                Serial.println("Котёл остановлен по времени! Текущее время: " + String(hours) + ":" + String(minutes));
              }
              break;
            }
          }
        }

        if (boilerTimeModeStarted) { // если режим включения, или выключения бойлера запущен
          for (int index = 0; index < boilerTimestampsCount; index++) { // проверяем каждый элемент массива с временем включения, или выключения бойлера
            if ((hours + ":" + minutes) == boilerTimestamps[index]) { // если текущее время найдёно в массиве
              if (boilerTimestampTypes[index] == 1) { // если в это время нужно включить бойлер
                digitalWrite(BOILER_RELAY_PIN, HIGH); // замыкаем реле к которому подключен бойлер
                Serial.println("Бойлер запущен по времени! Текущее время: " + String(hours) + ":" + String(minutes));
              } else { // если в это время нужно выключить бойлер
                digitalWrite(BOILER_RELAY_PIN, LOW); // размыкаем реле к которому подключен бойлер
                Serial.println("Бойлер остановлен по времени! Текущее время: " + String(hours) + ":" + String(minutes));
              }
              break;
            }
          }
        }
      }
    }
  }
}

bool resetSettings() {
  bool resetButtonState = !digitalRead(RESET_BUTTON_PIN); // если кнопка для перехода в режим настройки параметров работы нажата
  if (resetButtonState && !resetButtonFlag && millis() - resetButtonMillis >= 100) { // если кнопка нажата
    resetButtonMillis = millis(); // записываем время нажатия кнопки в millis
    resetButtonFlag = true; // поднимаем флажок
    Serial.println("Кнопка сброса настроек нажата!");
  }

  if (!resetButtonState && resetButtonFlag && millis() - resetButtonMillis >= 250) { // если кнопка отпущена
    resetButtonMillis = millis(); // записываем время отпускания кнопки в millis
    resetButtonFlag = false; // опускаем флажок
    Serial.println("Кнопка сброса настроек отпущена!");
    return true;
  }

  if (resetButtonState && resetButtonFlag && millis() - resetButtonMillis >= 3000) { // если кнопка удерживается дольше 3 секунд
    Serial.println("Вы перешли в режим настройки параметров работы!");
    setSettingsMode = true; // записываем то, что режим настройки параметров работы запущен
    WiFi.softAP("Умное Отопление", ""); // запускаем WiFi точку для настройки параметров работы
    server.on("/", handleRoot); // устанавливаем функцию, которая будет срабатывать при получении данных от Web клиента
    server.begin(); // запускаем Web сервер для настройки параметров работы
    return true;
  }
  return false;
}

void startHeating(int heatingElements) { // функция для включения определённого количества тэнов котла
  digitalWrite(HEATING_PUMP_RELAY_PIN, HIGH); // запускаем насос
  if (maxHeatingElements == 1) { // если у вашего котла 1 тэн
    digitalWrite(FIRST_HEATING_ELEMENT_RELAY_PIN, HIGH); // замыкаем первое реле
  } else if (maxHeatingElements == 2) { // если у вашего котла 2 тэна
    if (heatingElements == 1) { // если вы запустили 1 тэн
      digitalWrite(FIRST_HEATING_ELEMENT_RELAY_PIN, HIGH); // замыкаем первое реле
      digitalWrite(SECOND_HEATING_ELEMENT_RELAY_PIN, LOW); // размыкаем второе реле
    } else { // если вы запустили 2 тэна
      digitalWrite(FIRST_HEATING_ELEMENT_RELAY_PIN, LOW);   // размыкаем первое реле
      digitalWrite(SECOND_HEATING_ELEMENT_RELAY_PIN, HIGH); // замыкаем второе реле
    }
  } else if (maxHeatingElements == 3) { // если у вашего котла 3 тэна
    if (heatingElements == 1) { // если вы запустили 1 тэн
      digitalWrite(FIRST_HEATING_ELEMENT_RELAY_PIN, HIGH); // замыкаем первое реле
      digitalWrite(SECOND_HEATING_ELEMENT_RELAY_PIN, LOW); // размыкаем второе реле
      digitalWrite(THIRD_HEATING_ELEMENT_RELAY_PIN, LOW);  // размыкаем третье реле
    } else if (heatingElements == 2) { // если вы запустили 2 тэна
      digitalWrite(FIRST_HEATING_ELEMENT_RELAY_PIN, LOW);   // размыкаем первое реле
      digitalWrite(SECOND_HEATING_ELEMENT_RELAY_PIN, HIGH); // замыкаем второе реле
      digitalWrite(THIRD_HEATING_ELEMENT_RELAY_PIN, LOW);   // размыкаем третье реле
    } else if (heatingElements == 3) { // если вы запустили 3 тэна
      digitalWrite(FIRST_HEATING_ELEMENT_RELAY_PIN, LOW);  // размыкаем первое реле
      digitalWrite(SECOND_HEATING_ELEMENT_RELAY_PIN, LOW); // размыкаем второе реле
      digitalWrite(THIRD_HEATING_ELEMENT_RELAY_PIN, HIGH); // размыкаем третье реле
    }
  }
}

void stopHeating() { // функция для выключения котла
  digitalWrite(HEATING_PUMP_RELAY_PIN, LOW);
  if (maxHeatingElements == 1) {
    digitalWrite(FIRST_HEATING_ELEMENT_RELAY_PIN, LOW);
  } else if (maxHeatingElements == 2) {
    digitalWrite(FIRST_HEATING_ELEMENT_RELAY_PIN, LOW);
    digitalWrite(SECOND_HEATING_ELEMENT_RELAY_PIN, LOW);
  } else if (maxHeatingElements == 3) {
    digitalWrite(FIRST_HEATING_ELEMENT_RELAY_PIN, LOW);
    digitalWrite(SECOND_HEATING_ELEMENT_RELAY_PIN, LOW);
    digitalWrite(THIRD_HEATING_ELEMENT_RELAY_PIN, LOW);
  }
}

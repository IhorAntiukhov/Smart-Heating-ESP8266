// основная часть HTML страницы для настройки параметров работы
const char MainWebPage[] PROGMEM = R"rawliteral(
<HTML>
  <HEAD>
      <TITLE>Умное Отопление</TITLE>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <style>
          input[type="text"] {
              border-color: #00D679;
              border-width: 3px;
              border-style: solid;
              border-radius: 10px;
              font-size: 25px;
              font-family: monospace;
              font-weight: bold;
              margin-top: -10px;
              color: #7CD6A3;
              transition: 0.5s;
              opacity: 1;
              padding-left: 6px;
              padding-right: 6px;
          }
          input[type="password"] {
              border-color: #00D679;
              border-width: 3px;
              border-style: solid;
              border-radius: 10px;
              font-size: 25px;
              font-family: monospace;
              font-weight: bold;
              margin-top: -10px;
              color: #7CD6A3;
              transition: 0.5s;
              opacity: 1;
              padding-left: 6px;
              padding-right: 6px;
          }
          input[type="email"] {
              border-color: #00D679;
              border-width: 3px;
              border-style: solid;
              border-radius: 10px;
              font-size: 25px;
              font-family: monospace;
              font-weight: bold;
              margin-top: -10px;
              color: #7CD6A3;
              transition: 0.5s;
              opacity: 1;
              padding-left: 6px;
              padding-right: 6px;
          }
          input[type="number"] {
              border-color: #00D679;
              border-width: 3px;
              border-style: solid;
              border-radius: 10px;
              font-size: 25px;
              font-family: monospace;
              font-weight: bold;
              margin-top: -10px;
              color: #7CD6A3;
              transition: 0.5s;
              opacity: 1;
              padding-left: 6px;
              padding-right: 6px;
          }
          input[type="text"]::-webkit-input-placeholder {
              color: #00D679              
          }
          input[type="text"]:focus, input:focus {
              border-color: #7CD6A3;
              outline: none;
          }
          input[type="email"]::-webkit-input-placeholder {
              color: #00D679              
          }
          input[type="email"]:focus, input:focus {
              border-color: #7CD6A3;
              outline: none;
          }
          input[type="number"]::-webkit-input-placeholder {
              color: #00D679           
          }
          input[type="number"]:focus, input:focus {
              border-color: #7CD6A3;
              outline: none;
          }
          input[type="password"]::-webkit-input-placeholder {
              color: #00D679            
          }
          input:-webkit-autofill { 
              -webkit-box-shadow: 0 0 0 30px white inset !important;
              -webkit-text-fill-color: #7CD6A3;
          }
          .button {
              background-color: transparent;
              border-color: #00D679;
              border-style: solid;
              border-width: 3px;
              border-radius: 10px;
              font-size: 28px;
              font-family: monospace;
              opacity: 1;
              transition: 0.5s;
              font-weight: bold;
              color: #00D679;
          }
          .button:hover {
              opacity: 1;
              transition: 0.5s;
              border-color: #7CD6A3;
              color: #7CD6A3;
          }
          .button:focus {
              transition: 0.15s;
              border-color: #00D679;
              color: #00D679;
          }
          .toggle_button {
              background-color: transparent;
              border: none;
              cursor: pointer;
              padding: 0.5rem;
              margin-top: -14px;
              margin-left: -43px;
          }
      </style>
  </HEAD>
  <BODY>
    <CENTER>
        <h1 style="font-family:Helvetica;font-size:36px;margin-top:16px;color:#00D679;user-select:none;margin-bottom:-4px">Умное Отопление</h1>
        <form>
)rawliteral";

// кнопка для того, чтобы скрыть, или показать пароль вашей WiFi сети
const char ToggleSsidPass[] PROGMEM = R"rawliteral(
<button type="button" class="toggle_button" id="toggle_ssid_pass">
  <svg version="1.0" xmlns="http://www.w3.org/2000/svg"
    width="16.000000pt" height="16.000000pt" viewBox="0 0 512.000000 512.000000" preserveAspectRatio="xMidYMid meet">
      <g transform="translate(0.000000,512.000000) scale(0.100000,-0.100000)" fill="#00D679" stroke="none">
        <path d="M2400 4259 c-859 -51 -1655 -536 -2184 -1332 -52 -78 -122 -193 -155 -254 l-61 -113 61 -112 c433 -797 1156 -1364 1959 -1536 336 -73 744 -73 1080 0 715 153 1357 610 1804 1281 52 78 122 193 155 255 l61 112 -61 113 c-311 571 -788 1042 -1327 1311 -423 211 -867 303 -1332 275z m365 -655 c323 -62 599 -272 747 -569 77 -155 110 -296 110 -475 0 -282 -97 -529 -287 -729 -209 -220 -471 -333 -775 -333 -515 0 -938 347 -1045 857 -22 106 -22 304 0 410 44 208 131 377 270 524 88 93 163 151 268 206 230 122 458 157 712 109z"/><path d="M2421 3185 c-178 -39 -348 -172 -429 -336 -95 -193 -95 -385 0 -578 82 -166 251 -297 434 -336 451 -96 855 308 759 759 -72 342 -420 565 -764 491z"/>
      </g>
  </svg>
</button></p>
)rawliteral";

// кнопка для того, чтобы скрыть, или показать пароль вашего пользователя
const char ToggleUserPass[] PROGMEM = R"rawliteral(
<button type="button" class="toggle_button" id="toggle_user_pass">
  <svg version="1.0" xmlns="http://www.w3.org/2000/svg"
    width="16.000000pt" height="16.000000pt" viewBox="0 0 512.000000 512.000000" preserveAspectRatio="xMidYMid meet">
      <g transform="translate(0.000000,512.000000) scale(0.100000,-0.100000)" fill="#00D679" stroke="none">
        <path d="M2400 4259 c-859 -51 -1655 -536 -2184 -1332 -52 -78 -122 -193 -155 -254 l-61 -113 61 -112 c433 -797 1156 -1364 1959 -1536 336 -73 744 -73 1080 0 715 153 1357 610 1804 1281 52 78 122 193 155 255 l61 112 -61 113 c-311 571 -788 1042 -1327 1311 -423 211 -867 303 -1332 275z m365 -655 c323 -62 599 -272 747 -569 77 -155 110 -296 110 -475 0 -282 -97 -529 -287 -729 -209 -220 -471 -333 -775 -333 -515 0 -938 347 -1045 857 -22 106 -22 304 0 410 44 208 131 377 270 524 88 93 163 151 268 206 230 122 458 157 712 109z"/><path d="M2421 3185 c-178 -39 -348 -172 -429 -336 -95 -193 -95 -385 0 -578 82 -166 251 -297 434 -336 451 -96 855 308 759 759 -72 342 -420 565 -764 491z"/>
      </g>
  </svg>
</button></p>
)rawliteral";

// скрипт для того, чтобы скрыть, или показать пароль
const char Script[] PROGMEM = R"rawliteral(
            <button style="margin-top:-9px" required class="button">Сохранить Параметры</button>
            
            <script>
                const toggleUserPass = document.querySelector('#toggle_user_pass');
                const userPass = document.querySelector('#user_pass');
                const toggleSsidPass = document.querySelector('#toggle_ssid_pass');
                const ssidPass = document.querySelector('#ssid_pass');
                
                toggleUserPass.addEventListener('click', function (e) {
                    const textType = userPass.getAttribute('type') === 'password' ? 'text' : 'password';
                    userPass.setAttribute('type', textType);
                });
                
                toggleSsidPass.addEventListener('click', function (e) {
                    const textType = ssidPass.getAttribute('type') === 'password' ? 'text' : 'password';
                    ssidPass.setAttribute('type', textType);
                });
            </script>
        </form>
    </CENTER> 
  </BODY>
</HTML>
)rawliteral";    

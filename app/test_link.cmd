@echo Test deep links to My WebView:
@echo test_link.cmd [web/...] or [local/...] or [sites/...]
@echo on
%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe shell am start -W -a android.intent.action.VIEW -d "mywebview://%1" net.mikespub.mywebview

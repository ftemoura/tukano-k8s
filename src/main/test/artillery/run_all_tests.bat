@echo off
setlocal enabledelayedexpansion

set ERROR_LOG=error_log.txt
set OUTPUT_LOG=output_log.txt
set errorOccurred=0

call :runTest "user POST CSV users" ./user/post_csv_users.yaml
if !errorOccurred! neq 0 exit /b 1

call :runTest "forbidden login" ./user/forbidden_login.yaml
if !errorOccurred! neq 0 exit /b 1

call :runTest "GET CSV users" ./user/get_csv_users.yaml
if !errorOccurred! neq 0 exit /b 1

call :runTest "search CSV users" ./user/search_csv_users.yaml
if !errorOccurred! neq 0 exit /b 1

call :runTest "update CSV users" ./user/update_csv_users.yaml
if !errorOccurred! neq 0 exit /b 1

call :runTest "delete CSV user" ./user/delete_csv_users.yaml
if !errorOccurred! neq 0 exit /b 1

call :runTest "user POST CSV users again" ./user/post_csv_users.yaml
if !errorOccurred! neq 0 exit /b 1

call :runTest "create short CSV users" ./shorts/create_short_csv_users.yaml
if !errorOccurred! neq 0 exit /b 1

call :runTest "delete shorts" ./shorts/delete_shorts.yaml
if !errorOccurred! neq 0 exit /b 1

call :runTest "delete CSV users again" ./user/delete_csv_users.yaml
if !errorOccurred! neq 0 exit /b 1

call :runTest "user POST CSV users again" ./user/post_csv_users.yaml
if !errorOccurred! neq 0 exit /b 1

call :runTest "get followers" ./shorts/get_followers.yaml
if !errorOccurred! neq 0 exit /b 1

call :runTest "get short" ./shorts/get_short.yaml
if !errorOccurred! neq 0 exit /b 1

call :runTest "get shorts" ./shorts/get_shorts.yaml
if !errorOccurred! neq 0 exit /b 1

call :runTest "like short" ./shorts/like.yaml
if !errorOccurred! neq 0 exit /b 1

call :runTest "create short CSV users again" ./shorts/create_short_csv_users.yaml
if !errorOccurred! neq 0 exit /b 1

call :runTest "delete CSV users by ID" ./user/delete_csv_users_by_id.yaml
if !errorOccurred! neq 0 exit /b 1

echo All tests completed successfully.
exit /b 0

:runTest
echo Running %~1 test...
call artillery run %~2 > %OUTPUT_LOG% 2> %ERROR_LOG%
type %OUTPUT_LOG%
if exist %OUTPUT_LOG% (
    type %OUTPUT_LOG% | findstr /i "not ok" >nul
    if !errorlevel! equ 0 (
        echo Error found in %~1 test.
        set errorOccurred=1
        exit /b 1
    )
    type %OUTPUT_LOG% | findstr /i "Error" >nul
    if !errorlevel! equ 0 (
        echo Error found in %~1 test.
        set errorOccurred=1
        exit /b 1
    )
)
exit /b 0

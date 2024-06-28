#!/bin/bash

# Variables
KEY_PATH="/c/Users/OMEN/.ssh/mafia"
LOCAL_FILE="D:/Here We Go/MAFIA VER 2/build/libs/com.mafia2.mafia-ver2-all.jar"
REMOTE_USER="root"
REMOTE_HOST="34.47.132.185"
REMOTE_DIR="/root/mafiagame"
REMOTE_FILE="${REMOTE_DIR}/com.mafia2.mafia-ver2-all.jar"
TARGET_FILE="${REMOTE_DIR}/mafia.jar"
SERVICE_NAME="mafia"

# Copy the jar file to the remote machine
scp -i "$KEY_PATH" "$LOCAL_FILE" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR/"

# Execute commands on the remote machine
ssh -i "$KEY_PATH" "$REMOTE_USER@$REMOTE_HOST" << EOF
    # Rename the jar file
    mv -f "$REMOTE_FILE" "$TARGET_FILE"

    # Restart the service
    systemctl restart $SERVICE_NAME

    # Log the output of the service continuously
    journalctl -fu $SERVICE_NAME
EOF

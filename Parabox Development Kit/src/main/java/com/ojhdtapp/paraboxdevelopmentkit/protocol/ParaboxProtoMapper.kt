package com.ojhdtapp.paraboxdevelopmentkit.protocol

import android.net.Uri
import com.ojhdtapp.parabox.protocol.v2.ActionConfigProto
import com.ojhdtapp.parabox.protocol.v2.ActionResult
import com.ojhdtapp.parabox.protocol.v2.AnnotatedTextElementProto
import com.ojhdtapp.parabox.protocol.v2.AtAllElementProto
import com.ojhdtapp.parabox.protocol.v2.AtElementProto
import com.ojhdtapp.parabox.protocol.v2.AudioElementProto
import com.ojhdtapp.parabox.protocol.v2.BasicInfoProto
import com.ojhdtapp.parabox.protocol.v2.CategoryConfigProto
import com.ojhdtapp.parabox.protocol.v2.ChatProto
import com.ojhdtapp.parabox.protocol.v2.ConfigItemProto
import com.ojhdtapp.parabox.protocol.v2.ConfigValueProto
import com.ojhdtapp.parabox.protocol.v2.ConnectionStateProto
import com.ojhdtapp.parabox.protocol.v2.ContactProto
import com.ojhdtapp.parabox.protocol.v2.DriveRemoteResourceProto
import com.ojhdtapp.parabox.protocol.v2.EmptyResourceProto
import com.ojhdtapp.parabox.protocol.v2.ExternalAuthStepProto
import com.ojhdtapp.parabox.protocol.v2.FileElementProto
import com.ojhdtapp.parabox.protocol.v2.ForwardElementProto
import com.ojhdtapp.parabox.protocol.v2.ForwardNodeProto
import com.ojhdtapp.parabox.protocol.v2.ImageElementProto
import com.ojhdtapp.parabox.protocol.v2.InfoStepProto
import com.ojhdtapp.parabox.protocol.v2.InitStepProto
import com.ojhdtapp.parabox.protocol.v2.KeyboardTypeProto
import com.ojhdtapp.parabox.protocol.v2.LocationElementProto
import com.ojhdtapp.parabox.protocol.v2.MessageElementProto
import com.ojhdtapp.parabox.protocol.v2.PlainTextElementProto
import com.ojhdtapp.parabox.protocol.v2.QuoteReplyElementProto
import com.ojhdtapp.parabox.protocol.v2.ReceiveMessageProto
import com.ojhdtapp.parabox.protocol.v2.ResourceInfoProto
import com.ojhdtapp.parabox.protocol.v2.SelectConfigProto
import com.ojhdtapp.parabox.protocol.v2.SelectStepProto
import com.ojhdtapp.parabox.protocol.v2.SendMessageProto
import com.ojhdtapp.parabox.protocol.v2.StatusChangedFrame
import com.ojhdtapp.parabox.protocol.v2.SwitchConfigProto
import com.ojhdtapp.parabox.protocol.v2.SwitchStepProto
import com.ojhdtapp.parabox.protocol.v2.SyncedResourceProto
import com.ojhdtapp.parabox.protocol.v2.TextInputConfigProto
import com.ojhdtapp.parabox.protocol.v2.TextInputStepProto
import com.ojhdtapp.parabox.protocol.v2.TextInputWithImageStepProto
import com.ojhdtapp.parabox.protocol.v2.UnsupportedElementProto
import com.ojhdtapp.parabox.protocol.v2.UriLocalResourceProto
import com.ojhdtapp.parabox.protocol.v2.UrlRemoteResourceProto
import com.ojhdtapp.parabox.protocol.v2.VideoElementProto
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxBasicInfo
import com.ojhdtapp.paraboxdevelopmentkit.model.ParaboxResult
import com.ojhdtapp.paraboxdevelopmentkit.model.ReceiveMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.SendMessage
import com.ojhdtapp.paraboxdevelopmentkit.model.chat.ParaboxChat
import com.ojhdtapp.paraboxdevelopmentkit.model.config_item.ParaboxConfigItem
import com.ojhdtapp.paraboxdevelopmentkit.model.contact.ParaboxContact
import com.ojhdtapp.paraboxdevelopmentkit.model.init_actions.ParaboxInitAction
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAnnotatedText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAt
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAtAll
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxAudio
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxFile
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxForward
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxImage
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxLocation
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxMessageElement
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxPlainText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxQuoteReply
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxText
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxUnsupported
import com.ojhdtapp.paraboxdevelopmentkit.model.message.ParaboxVideo
import com.ojhdtapp.paraboxdevelopmentkit.model.res_info.ParaboxResourceInfo
import com.ojhdtapp.paraboxdevelopmentkit.v2.ParaboxConnectionStatus

object ParaboxProtoMapper {

    // ---- ActionResult ----

    fun ParaboxResult.toProto(): ActionResult {
        val uuid = messageUuid
        return ActionResult.newBuilder()
            .setCode(code)
            .setMessage(message)
            .apply {
                if (!uuid.isNullOrBlank()) {
                    setMessageUuid(uuid)
                }
            }
            .build()
    }

    fun ActionResult.toDomain(): ParaboxResult = ParaboxResult(
        code = code,
        message = message,
        messageUuid = messageUuid.ifBlank { null },
    )

    // ---- ConnectionStatus ----

    fun ParaboxConnectionStatus.toProto(): StatusChangedFrame {
        val builder = StatusChangedFrame.newBuilder()
        when (this) {
            is ParaboxConnectionStatus.Pending -> builder.setState(ConnectionStateProto.CONNECTION_STATE_PENDING)
            is ParaboxConnectionStatus.Initializing -> builder.setState(ConnectionStateProto.CONNECTION_STATE_INITIALIZING)
            is ParaboxConnectionStatus.Active -> builder.setState(ConnectionStateProto.CONNECTION_STATE_ACTIVE)
            is ParaboxConnectionStatus.Reconnecting -> {
                builder.setState(ConnectionStateProto.CONNECTION_STATE_RECONNECTING)
                builder.setRetryAttempt(attempt)
                builder.setMessage(message)
            }
            is ParaboxConnectionStatus.Error -> {
                builder.setState(ConnectionStateProto.CONNECTION_STATE_ERROR)
                builder.setMessage(message)
            }
        }
        return builder.build()
    }

    fun StatusChangedFrame.toDomain(): ParaboxConnectionStatus = when (state) {
        ConnectionStateProto.CONNECTION_STATE_PENDING -> ParaboxConnectionStatus.Pending
        ConnectionStateProto.CONNECTION_STATE_INITIALIZING -> ParaboxConnectionStatus.Initializing
        ConnectionStateProto.CONNECTION_STATE_ACTIVE -> ParaboxConnectionStatus.Active
        ConnectionStateProto.CONNECTION_STATE_RECONNECTING -> ParaboxConnectionStatus.Reconnecting(retryAttempt, message)
        ConnectionStateProto.CONNECTION_STATE_ERROR, ConnectionStateProto.UNRECOGNIZED ->
            ParaboxConnectionStatus.Error(message.ifEmpty { "Unknown error" })
    }

    // ---- ResourceInfo ----

    fun ParaboxResourceInfo.toProto(): ResourceInfoProto {
        val builder = ResourceInfoProto.newBuilder()
        when (this) {
            is ParaboxResourceInfo.ParaboxEmptyInfo -> {
                builder.setEmpty(EmptyResourceProto.getDefaultInstance())
            }
            is ParaboxResourceInfo.ParaboxSyncedInfo -> {
                builder.setSynced(
                    SyncedResourceProto.newBuilder()
                        .setLocal(local.toProto())
                        .setRemote(remote.toProto())
                        .build()
                )
            }
            is ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo -> {
                builder.setUriLocal(
                    UriLocalResourceProto.newBuilder()
                        .setUri(uri.toString())
                        .build()
                )
            }
            is ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo -> {
                builder.setUrlRemote(
                    UrlRemoteResourceProto.newBuilder()
                        .setUrl(url)
                        .build()
                )
            }
            is ParaboxResourceInfo.ParaboxRemoteInfo.DriveRemoteInfo -> {
                builder.setDriveRemote(
                    DriveRemoteResourceProto.newBuilder()
                        .setUuid(uuid)
                        .setCloudPath(cloudPath)
                        .setDriveType(driveType)
                        .build()
                )
            }
        }
        return builder.build()
    }

    fun ResourceInfoProto.toDomain(): ParaboxResourceInfo = when (resourceCase) {
        ResourceInfoProto.ResourceCase.URI_LOCAL ->
            ParaboxResourceInfo.ParaboxLocalInfo.UriLocalInfo(Uri.parse(uriLocal.uri))
        ResourceInfoProto.ResourceCase.URL_REMOTE ->
            ParaboxResourceInfo.ParaboxRemoteInfo.UrlRemoteInfo(urlRemote.url)
        ResourceInfoProto.ResourceCase.DRIVE_REMOTE ->
            ParaboxResourceInfo.ParaboxRemoteInfo.DriveRemoteInfo(
                uuid = driveRemote.uuid,
                cloudPath = driveRemote.cloudPath,
                driveType = driveRemote.driveType,
            )
        ResourceInfoProto.ResourceCase.SYNCED -> {
            val local = synced.local.toDomain() as? ParaboxResourceInfo.ParaboxLocalInfo
            val remote = synced.remote.toDomain() as? ParaboxResourceInfo.ParaboxRemoteInfo
            if (local != null && remote != null) {
                ParaboxResourceInfo.ParaboxSyncedInfo(local, remote)
            } else {
                local ?: remote ?: ParaboxResourceInfo.ParaboxEmptyInfo
            }
        }
        ResourceInfoProto.ResourceCase.EMPTY,
        ResourceInfoProto.ResourceCase.RESOURCE_NOT_SET,
        null -> ParaboxResourceInfo.ParaboxEmptyInfo
    }

    // ---- BasicInfo / Contact / Chat ----

    fun ParaboxBasicInfo.toProto(): BasicInfoProto {
        val src = this
        return BasicInfoProto.newBuilder()
            .apply {
                if (src.name != null) {
                    setHasName(true)
                    setName(src.name)
                }
                setAvatar(src.avatar.toProto())
            }
            .build()
    }

    fun BasicInfoProto.toDomain(): ParaboxBasicInfo = ParaboxBasicInfo(
        name = if (hasName) name else null,
        avatar = if (hasAvatar()) avatar.toDomain() else ParaboxResourceInfo.ParaboxEmptyInfo,
    )

    fun ParaboxContact.toProto(): ContactProto = ContactProto.newBuilder()
        .setBasicInfo(basicInfo.toProto())
        .setUid(uid)
        .build()

    fun ContactProto.toDomain(): ParaboxContact = ParaboxContact(
        basicInfo = if (hasBasicInfo()) basicInfo.toDomain() else ParaboxBasicInfo(null, ParaboxResourceInfo.ParaboxEmptyInfo),
        uid = uid,
    )

    fun ParaboxChat.toProto(): ChatProto = ChatProto.newBuilder()
        .setBasicInfo(basicInfo.toProto())
        .setType(type)
        .setUid(uid)
        .build()

    fun ChatProto.toDomain(): ParaboxChat = ParaboxChat(
        basicInfo = if (hasBasicInfo()) basicInfo.toDomain() else ParaboxBasicInfo(null, ParaboxResourceInfo.ParaboxEmptyInfo),
        type = type,
        uid = uid,
    )

    // ---- Message Elements ----

    fun ParaboxMessageElement.toProto(): MessageElementProto {
        val builder = MessageElementProto.newBuilder()
        when (this) {
            is ParaboxPlainText -> builder.setPlainText(
                PlainTextElementProto.newBuilder().setText(text).build()
            )
            is ParaboxAt -> builder.setAt(
                AtElementProto.newBuilder().setTarget(target.toProto()).build()
            )
            is ParaboxAtAll -> builder.setAtAll(AtAllElementProto.getDefaultInstance())
            is ParaboxImage -> builder.setImage(
                ImageElementProto.newBuilder()
                    .setWidth(width)
                    .setHeight(height)
                    .also { b ->
                        if (fileName != null) {
                            b.setHasFileName(true)
                            b.setFileName(fileName)
                        }
                    }
                    .setResourceInfo(resourceInfo.toProto())
                    .build()
            )
            is ParaboxAudio -> builder.setAudio(
                AudioElementProto.newBuilder()
                    .setLength(length)
                    .setFileSize(fileSize)
                    .also { b ->
                        if (fileName != null) {
                            b.setHasFileName(true)
                            b.setFileName(fileName)
                        }
                    }
                    .setResourceInfo(resourceInfo.toProto())
                    .build()
            )
            is ParaboxVideo -> builder.setVideo(
                VideoElementProto.newBuilder()
                    .setLength(length)
                    .setFileSize(fileSize)
                    .also { b ->
                        if (fileName != null) {
                            b.setHasFileName(true)
                            b.setFileName(fileName)
                        }
                    }
                    .setResourceInfo(resourceInfo.toProto())
                    .build()
            )
            is ParaboxFile -> builder.setFile(
                FileElementProto.newBuilder()
                    .setName(name)
                    .setExtension(extension)
                    .setSize(size)
                    .setLastModifiedTime(lastModifiedTime)
                    .setResourceInfo(resourceInfo.toProto())
                    .build()
            )
            is ParaboxLocation -> builder.setLocation(
                LocationElementProto.newBuilder()
                    .setLatitude(latitude)
                    .setLongitude(longitude)
                    .also { b ->
                        if (name != null) {
                            b.setHasName(true)
                            b.setName(name)
                        }
                        if (description != null) {
                            b.setHasDescription(true)
                            b.setDescription(description)
                        }
                    }
                    .build()
            )
            is ParaboxQuoteReply -> {
                val src = this
                builder.setQuoteReply(
                    QuoteReplyElementProto.newBuilder()
                        .apply {
                            if (src.sender != null) setSender(src.sender.toProto())
                            if (src.timestamp != null) {
                                setHasTimestamp(true)
                                setTimestamp(src.timestamp)
                            }
                            if (src.id != null) {
                                setHasId(true)
                                setId(src.id)
                            }
                            addAllMessages(src.messages.map { it.toProto() })
                        }
                        .build()
                )
            }
            is ParaboxForward -> builder.setForward(
                ForwardElementProto.newBuilder()
                    .addAllNodes(list.map { it.toProto() })
                    .build()
            )
            is ParaboxAnnotatedText -> builder.setAnnotatedText(
                AnnotatedTextElementProto.newBuilder()
                    .addAllItems(list.map { it.toProto() })
                    .build()
            )
            is ParaboxUnsupported -> builder.setUnsupported(UnsupportedElementProto.getDefaultInstance())
        }
        return builder.build()
    }

    fun MessageElementProto.toDomain(): ParaboxMessageElement = when (elementCase) {
        MessageElementProto.ElementCase.PLAIN_TEXT -> ParaboxPlainText(plainText.text)
        MessageElementProto.ElementCase.AT -> ParaboxAt(at.target.toDomain())
        MessageElementProto.ElementCase.AT_ALL -> ParaboxAtAll
        MessageElementProto.ElementCase.IMAGE -> ParaboxImage(
            width = image.width,
            height = image.height,
            fileName = if (image.hasFileName) image.fileName else null,
            resourceInfo = if (image.hasResourceInfo()) image.resourceInfo.toDomain() else ParaboxResourceInfo.ParaboxEmptyInfo,
        )
        MessageElementProto.ElementCase.AUDIO -> ParaboxAudio(
            length = audio.length,
            fileName = if (audio.hasFileName) audio.fileName else null,
            fileSize = audio.fileSize,
            resourceInfo = if (audio.hasResourceInfo()) audio.resourceInfo.toDomain() else ParaboxResourceInfo.ParaboxEmptyInfo,
        )
        MessageElementProto.ElementCase.VIDEO -> ParaboxVideo(
            length = video.length,
            fileName = if (video.hasFileName) video.fileName else null,
            fileSize = video.fileSize,
            resourceInfo = if (video.hasResourceInfo()) video.resourceInfo.toDomain() else ParaboxResourceInfo.ParaboxEmptyInfo,
        )
        MessageElementProto.ElementCase.FILE -> ParaboxFile(
            name = file.name,
            extension = file.extension,
            size = file.size,
            lastModifiedTime = file.lastModifiedTime,
            resourceInfo = if (file.hasResourceInfo()) file.resourceInfo.toDomain() else ParaboxResourceInfo.ParaboxEmptyInfo,
        )
        MessageElementProto.ElementCase.LOCATION -> ParaboxLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            name = if (location.hasName) location.name else null,
            description = if (location.hasDescription) location.description else null,
        )
        MessageElementProto.ElementCase.QUOTE_REPLY -> ParaboxQuoteReply(
            sender = if (quoteReply.hasSender()) quoteReply.sender.toDomain() else null,
            timestamp = if (quoteReply.hasTimestamp) quoteReply.timestamp else null,
            id = if (quoteReply.hasId) quoteReply.id else null,
            messages = quoteReply.messagesList.map { it.toDomain() },
        )
        MessageElementProto.ElementCase.FORWARD -> ParaboxForward(
            list = forward.nodesList.map { it.toDomain() },
        )
        MessageElementProto.ElementCase.ANNOTATED_TEXT -> ParaboxAnnotatedText(
            list = annotatedText.itemsList.mapNotNull { it.toDomain() as? ParaboxText },
        )
        MessageElementProto.ElementCase.UNSUPPORTED,
        MessageElementProto.ElementCase.ELEMENT_NOT_SET,
        null -> ParaboxUnsupported
    }

    fun ParaboxForward.ForwardNode.toProto(): ForwardNodeProto {
        val src = this
        return ForwardNodeProto.newBuilder()
            .apply {
                if (src.sender != null) setSender(src.sender.toProto())
                if (src.timestamp != null) {
                    setHasTimestamp(true)
                    setTimestamp(src.timestamp)
                }
                if (src.id != null) {
                    setHasId(true)
                    setId(src.id)
                }
                addAllMessages(src.messages.map { it.toProto() })
            }
            .build()
    }

    fun ForwardNodeProto.toDomain(): ParaboxForward.ForwardNode = ParaboxForward.ForwardNode(
        sender = if (hasSender()) sender.toDomain() else null,
        timestamp = if (hasTimestamp) timestamp else null,
        id = if (hasId) id else null,
        messages = messagesList.map { it.toDomain() },
    )

    // ---- ReceiveMessage & SendMessage ----

    fun ReceiveMessage.toProto(): ReceiveMessageProto = ReceiveMessageProto.newBuilder()
        .addAllContents(contents.map { it.toProto() })
        .setSender(sender.toProto())
        .setChat(chat.toProto())
        .setTimestamp(timestamp)
        .setUuid(uuid)
        .build()

    fun ReceiveMessageProto.toDomain(): ReceiveMessage = ReceiveMessage(
        contents = contentsList.map { it.toDomain() },
        sender = sender.toDomain(),
        chat = chat.toDomain(),
        timestamp = timestamp,
        uuid = uuid,
    )

    fun SendMessage.toProto(): SendMessageProto = SendMessageProto.newBuilder()
        .addAllContents(contents.map { it.toProto() })
        .setChat(chat.toProto())
        .setTimestamp(timestamp)
        .setUuid(uuid)
        .build()

    fun SendMessageProto.toDomain(): SendMessage = SendMessage(
        contents = contentsList.map { it.toDomain() },
        chat = chat.toDomain(),
        timestamp = timestamp,
        uuid = uuid,
    )

    // ---- KeyboardType / InitAction / ConfigItem / ConfigValue ----

    fun ParaboxInitAction.KeyboardType.toProto(): KeyboardTypeProto = when (this) {
        ParaboxInitAction.KeyboardType.TEXT -> KeyboardTypeProto.KEYBOARD_TEXT
        ParaboxInitAction.KeyboardType.NUMBER -> KeyboardTypeProto.KEYBOARD_NUMBER
        ParaboxInitAction.KeyboardType.EMAIL -> KeyboardTypeProto.KEYBOARD_EMAIL
        ParaboxInitAction.KeyboardType.PASSWORD -> KeyboardTypeProto.KEYBOARD_PASSWORD
    }

    fun KeyboardTypeProto.toDomain(): ParaboxInitAction.KeyboardType = when (this) {
        KeyboardTypeProto.KEYBOARD_NUMBER -> ParaboxInitAction.KeyboardType.NUMBER
        KeyboardTypeProto.KEYBOARD_EMAIL -> ParaboxInitAction.KeyboardType.EMAIL
        KeyboardTypeProto.KEYBOARD_PASSWORD -> ParaboxInitAction.KeyboardType.PASSWORD
        KeyboardTypeProto.KEYBOARD_TEXT, KeyboardTypeProto.UNRECOGNIZED -> ParaboxInitAction.KeyboardType.TEXT
    }

    fun ParaboxInitAction.toProto(): InitStepProto {
        val builder = InitStepProto.newBuilder()
            .setKey(key)
            .setTitle(title)
            .setDescription(description)
            .setErrMsg(errMsg)
        when (this) {
            is ParaboxInitAction.InfoAction -> builder.setInfo(InfoStepProto.getDefaultInstance())
            is ParaboxInitAction.TextInputAction -> builder.setTextInput(
                TextInputStepProto.newBuilder()
                    .setLabel(label)
                    .setKeyboardType(type.toProto())
                    .also { b ->
                        if (defaultValue != null) {
                            b.setHasDefaultValue(true)
                            b.setDefaultValue(defaultValue)
                        }
                    }
                    .build()
            )
            is ParaboxInitAction.SelectAction -> builder.setSelect(
                SelectStepProto.newBuilder()
                    .addAllOptions(options)
                    .also { b ->
                        if (defaultValue != null) {
                            b.setHasDefaultValue(true)
                            b.setDefaultValue(defaultValue)
                        }
                    }
                    .build()
            )
            is ParaboxInitAction.TextInputWithImageAction -> builder.setTextInputWithImage(
                TextInputWithImageStepProto.newBuilder()
                    .setImage(image.toProto())
                    .setLabel(label)
                    .setKeyboardType(type.toProto())
                    .also { b ->
                        if (defaultValue != null) {
                            b.setHasDefaultValue(true)
                            b.setDefaultValue(defaultValue)
                        }
                    }
                    .build()
            )
            is ParaboxInitAction.SwitchAction -> builder.setSwitchStep(
                SwitchStepProto.newBuilder().setDefaultValue(defaultValue).build()
            )
            is ParaboxInitAction.ExternalAuthAction -> builder.setExternalAuth(
                ExternalAuthStepProto.newBuilder()
                    .setActivityAction(activityAction)
                    .setTargetPackage(targetPackage)
                    .setAuthUrl(authUrl)
                    .setButtonLabel(buttonLabel)
                    .build()
            )
        }
        return builder.build()
    }

    fun InitStepProto.toDomain(): ParaboxInitAction = when (stepTypeCase) {
        InitStepProto.StepTypeCase.TEXT_INPUT -> ParaboxInitAction.TextInputAction(
            key = key,
            title = title,
            errMsg = errMsg,
            description = description,
            label = textInput.label,
            type = textInput.keyboardType.toDomain(),
            defaultValue = if (textInput.hasDefaultValue) textInput.defaultValue else null,
        )
        InitStepProto.StepTypeCase.SELECT -> ParaboxInitAction.SelectAction(
            key = key,
            title = title,
            errMsg = errMsg,
            description = description,
            options = select.optionsList,
            defaultValue = if (select.hasDefaultValue) select.defaultValue else null,
        )
        InitStepProto.StepTypeCase.TEXT_INPUT_WITH_IMAGE -> ParaboxInitAction.TextInputWithImageAction(
            key = key,
            title = title,
            errMsg = errMsg,
            description = description,
            image = if (textInputWithImage.hasImage()) textInputWithImage.image.toDomain() else ParaboxResourceInfo.ParaboxEmptyInfo,
            label = textInputWithImage.label,
            type = textInputWithImage.keyboardType.toDomain(),
            defaultValue = if (textInputWithImage.hasDefaultValue) textInputWithImage.defaultValue else null,
        )
        InitStepProto.StepTypeCase.SWITCH_STEP -> ParaboxInitAction.SwitchAction(
            key = key,
            title = title,
            errMsg = errMsg,
            description = description,
            defaultValue = switchStep.defaultValue,
        )
        InitStepProto.StepTypeCase.EXTERNAL_AUTH -> ParaboxInitAction.ExternalAuthAction(
            key = key,
            title = title,
            errMsg = errMsg,
            description = description,
            activityAction = externalAuth.activityAction,
            targetPackage = externalAuth.targetPackage,
            authUrl = externalAuth.authUrl,
            buttonLabel = externalAuth.buttonLabel.ifEmpty { "前往授权" },
        )
        InitStepProto.StepTypeCase.INFO,
        InitStepProto.StepTypeCase.STEPTYPE_NOT_SET,
        null -> ParaboxInitAction.InfoAction(
            key = key,
            title = title,
            errMsg = errMsg,
            description = description,
        )
    }

    fun ParaboxConfigItem.toProto(): ConfigItemProto {
        val builder = ConfigItemProto.newBuilder()
            .setKey(key)
            .setTitle(title)
            .setDescription(description)
        when (this) {
            is ParaboxConfigItem.Category -> builder.setCategory(CategoryConfigProto.getDefaultInstance())
            is ParaboxConfigItem.TextInputConfigItem -> builder.setTextInput(
                TextInputConfigProto.newBuilder()
                    .setLabel(label)
                    .setKeyboardType(type.toProto())
                    .also { b ->
                        if (defaultValue != null) {
                            b.setHasDefaultValue(true)
                            b.setDefaultValue(defaultValue)
                        }
                    }
                    .build()
            )
            is ParaboxConfigItem.SelectConfigItem -> builder.setSelect(
                SelectConfigProto.newBuilder()
                    .addAllOptions(options)
                    .also { b ->
                        if (defaultValue != null) {
                            b.setHasDefaultValue(true)
                            b.setDefaultValue(defaultValue)
                        }
                    }
                    .build()
            )
            is ParaboxConfigItem.SwitchConfigItem -> builder.setSwitchItem(
                SwitchConfigProto.newBuilder().setDefaultValue(defaultValue).build()
            )
            is ParaboxConfigItem.ActionConfigItem -> builder.setActionItem(
                ActionConfigProto.newBuilder()
                    .also { b ->
                        confirmModel?.let { cm ->
                            b.setHasConfirm(true)
                            b.setConfirmTitle(cm.title)
                            b.setConfirmDescription(cm.description)
                            b.setConfirmText(cm.confirmText)
                            b.setCancelText(cm.cancelText)
                        }
                    }
                    .build()
            )
        }
        return builder.build()
    }

    fun ConfigItemProto.toDomain(): ParaboxConfigItem = when (itemTypeCase) {
        ConfigItemProto.ItemTypeCase.TEXT_INPUT -> ParaboxConfigItem.TextInputConfigItem(
            key = key,
            title = title,
            description = description,
            label = textInput.label,
            type = textInput.keyboardType.toDomain(),
            defaultValue = if (textInput.hasDefaultValue) textInput.defaultValue else null,
        )
        ConfigItemProto.ItemTypeCase.SELECT -> ParaboxConfigItem.SelectConfigItem(
            key = key,
            title = title,
            description = description,
            options = select.optionsList,
            defaultValue = if (select.hasDefaultValue) select.defaultValue else null,
        )
        ConfigItemProto.ItemTypeCase.SWITCH_ITEM -> ParaboxConfigItem.SwitchConfigItem(
            key = key,
            title = title,
            description = description,
            defaultValue = switchItem.defaultValue,
        )
        ConfigItemProto.ItemTypeCase.ACTION_ITEM -> ParaboxConfigItem.ActionConfigItem(
            key = key,
            title = title,
            description = description,
            confirmModel = if (actionItem.hasConfirm) {
                ParaboxConfigItem.ActionConfigItem.ConfirmModel(
                    title = actionItem.confirmTitle,
                    description = actionItem.confirmDescription,
                    confirmText = actionItem.confirmText.ifEmpty { "确定" },
                    cancelText = actionItem.cancelText.ifEmpty { "取消" },
                )
            } else {
                null
            },
        )
        ConfigItemProto.ItemTypeCase.CATEGORY,
        ConfigItemProto.ItemTypeCase.ITEMTYPE_NOT_SET,
        null -> ParaboxConfigItem.Category(
            key = key,
            title = title,
            description = description,
        )
    }

    fun toConfigValueProto(value: Any?): ConfigValueProto {
        val builder = ConfigValueProto.newBuilder()
        when (value) {
            is String -> builder.setStringValue(value)
            is Int -> builder.setIntValue(value)
            is Boolean -> builder.setBoolValue(value)
            null -> {}
            else -> builder.setStringValue(value.toString())
        }
        return builder.build()
    }

    fun ConfigValueProto.toDomainValue(): Any? = when (valueCase) {
        ConfigValueProto.ValueCase.STRING_VALUE -> stringValue
        ConfigValueProto.ValueCase.INT_VALUE -> intValue
        ConfigValueProto.ValueCase.BOOL_VALUE -> boolValue
        ConfigValueProto.ValueCase.JSON_VALUE -> jsonValue
        ConfigValueProto.ValueCase.VALUE_NOT_SET, null -> null
    }
}

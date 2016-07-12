package im.actor.core.modules.encryption;

import im.actor.core.api.updates.UpdateEncryptedPackage;
import im.actor.core.api.updates.UpdatePublicKeyGroupAdded;
import im.actor.core.api.updates.UpdatePublicKeyGroupRemoved;
import im.actor.core.entity.Message;
import im.actor.core.entity.MessageState;
import im.actor.core.entity.Peer;
import im.actor.core.entity.content.AbsContent;
import im.actor.core.modules.AbsModule;
import im.actor.core.modules.ModuleContext;
import im.actor.core.modules.sequence.processor.SequenceProcessor;
import im.actor.core.network.parser.Update;
import im.actor.runtime.actors.messages.Void;
import im.actor.runtime.promise.Promise;

public class EncryptionProcessor extends AbsModule implements SequenceProcessor {

    public EncryptionProcessor(ModuleContext context) {
        super(context);
    }

    @Override
    public Promise<Void> process(Update update) {
        if (update instanceof UpdatePublicKeyGroupAdded) {
            UpdatePublicKeyGroupAdded groupAdded = (UpdatePublicKeyGroupAdded) update;
            return context().getEncryption()
                    .getKeyManager()
                    .onKeyGroupAdded(groupAdded.getUid(), groupAdded.getKeyGroup());
        } else if (update instanceof UpdatePublicKeyGroupRemoved) {
            UpdatePublicKeyGroupRemoved groupRemoved = (UpdatePublicKeyGroupRemoved) update;
            return context().getEncryption()
                    .getKeyManager()
                    .onKeyGroupRemoved(groupRemoved.getUid(), groupRemoved.getKeyGroupId());
        } else if (update instanceof UpdateEncryptedPackage) {
            UpdateEncryptedPackage encryptedPackage = (UpdateEncryptedPackage) update;
            return context().getEncryption()
                    .decrypt(encryptedPackage.getSenderId(), encryptedPackage.getEncryptedBox())
                    .flatMap(message -> {
                        Message msg = new Message(encryptedPackage.getRandomId(),
                                encryptedPackage.getDate(), encryptedPackage.getDate(),
                                encryptedPackage.getSenderId(), MessageState.UNKNOWN,
                                AbsContent.fromMessage(message));
                        return context().getMessagesModule().getRouter()
                                .onNewMessage(Peer.secret(encryptedPackage.getSenderId()), msg);
                    });
        }
        return null;
    }
}
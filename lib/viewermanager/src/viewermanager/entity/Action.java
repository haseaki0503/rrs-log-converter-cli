package viewermanager.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import rescuecore2.messages.Command;
import rescuecore2.standard.messages.*;
import rescuecore2.worldmodel.EntityID;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Action - hold a communication or action of Agent act.
 * Agentが発した行動や通信などの動作を保持する
 * */
@JsonFormat(shape=JsonFormat.Shape.OBJECT)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Action {

    /**
     * Entity ID of take this action.
     * この行動を起こしたAgentのID
     * */
    public Integer id;

    /**
     * type of action : Look <code>EntityKey.Action</code>
     * Actionの種類 : <code>EntityKey.Action</code>を参照
     * */
    public String type;

    // Move, Clear
    /**
     * list of Areas IDs for wanted to move;
     * 移動したい経路情報となるAreaのIDリスト
     * */
    public List<Integer> path;

    /**
     * coordinate x of wanted to move
     * 移動したい先のx座標
     * */
    public Integer x;

    /**
     * coordinate y of wanted to move
     * 移動したい先のy座標
     * */
    public Integer y;

    // Radio
    /**
     * channels of radio communication
     * 無線通信のチャンネル
     * */
    public Integer channel;

    // Rescue, Load, Extinguish, Clear, LClear
    /**
     * act target's entity ID
     * 動作対象のEntity ID
     * */
    public Integer target;

    // Extinguish
    /**
     * quantity of water used for extinguish.
     * 消火に使用した水の量
     * */
    public Integer water;

    // Message Bytes
    /**
     * bytes of message communication.
     * 通信で扱われたバイト数
     * */
    public Integer messageSize;

    protected Action() {
        /// Set all members to null
        try {
            for (Field field : this.getClass().getFields()) {
                field.set(this, null);
            }
        }
        catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    public Action(Command command)
    {
        this();
        if(command instanceof AKMove) {
            AKMove move = (AKMove)command;
            this.type = EntityKey.Action.MOVE;
            this.path = move.getPath().stream().map(EntityID::getValue).collect(Collectors.toList());
            if(move.getDestinationX() >= 0) {
                this.x = move.getDestinationX();
            }
            if(move.getDestinationY() >= 0) {
                this.y = move.getDestinationY();
            }
        }
        else if(command instanceof AKRest) {
            this.type = EntityKey.Action.REST;
        }
        else if(command instanceof AKExtinguish) {
            AKExtinguish extinguish = (AKExtinguish)command;
            this.type = EntityKey.Action.EXTINGUISH;
            this.water = extinguish.getWater();
            this.target = extinguish.getTarget().getValue();
        }
        else if(command instanceof AKRescue) {
            AKRescue rescue = (AKRescue)command;
            this.type = EntityKey.Action.RESCUE;
            this.target = rescue.getTarget().getValue();
        }
        else if(command instanceof AKLoad) {
            AKLoad load = (AKLoad)command;
            this.type = EntityKey.Action.LOAD;
            this.target = load.getTarget().getValue();
        }
        else if (command instanceof AKUnload) {
            this.type = EntityKey.Action.UNLOAD;
        }
        else if(command instanceof AKClear) {
            /// LEGACY
            AKClear clear = (AKClear)command;
            this.type = EntityKey.Action.LCLEAR;
            this.target = clear.getTarget().getValue();
        }
        else if(command instanceof AKClearArea) {
            /// New
            AKClearArea clearArea = (AKClearArea)command;
            this.type = EntityKey.Action.CLEAR;
            this.x = clearArea.getDestinationX();
            this.y = clearArea.getDestinationY();
        }
        else if(command instanceof AKSay) {
            /// Voice
            this.type = EntityKey.Action.VOICE;
            this.messageSize = ((AKSay)command).getContent().length;
        }
        else if(command instanceof AKSpeak) {
            /// Radio
            this.type = EntityKey.Action.RADIO;
            this.channel = ((AKSpeak)command).getChannel();
            this.messageSize = ((AKSpeak)command).getContent().length;
        }
        else if(command instanceof AKSubscribe) {
            /// SUBSCRIBE
            this.type = EntityKey.Action.SUBSCRIBE;
        }
        else if(command instanceof AKTell) {
            /// TELL
            this.type = EntityKey.Action.TELL;
            this.messageSize = ((AKTell)command).getContent().length;
        }
        else {
            /// Unknown
            this.type = EntityKey.UNKNOWN;
        }

        this.id = command.getAgentID().getValue();
    }
}

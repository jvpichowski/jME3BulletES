package com.jvpichowski.examples.es.bullet.extension.character;

import com.jme3.app.FlyCamAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.debug.BulletDebugAppState;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jvpichowski.jme3.es.bullet.extension.character.PhysicsCharacter;
import com.jvpichowski.jme3.es.bullet.extension.character.PhysicsCharacterSystem;
import com.jvpichowski.jme3.es.bullet.extension.character.PhysicsCharacterMovement;
import com.jvpichowski.jme3.es.bullet.components.*;
import com.jvpichowski.jme3.states.CameraPositionPrinter;
import com.jvpichowski.jme3.states.DebugViewState;
import com.jvpichowski.jme3.states.ESBulletState;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.WatchedEntity;
import com.simsilica.es.base.DefaultEntityData;

/**
 * Created by jan on 20.02.2017.
 */
public class CharacterTest extends SimpleApplication implements ActionListener {

    public static void main(String[] args) {
        EntityData entityData = new DefaultEntityData();
        CharacterTest app = new CharacterTest(entityData);
        app.start();
    }

    private final EntityData entityData;
    private EntityId character;
    private BitmapText infoText;
    private WatchedEntity characterSpeed;

    public CharacterTest(EntityData entityData){
        super(new FlyCamAppState(), new ESBulletState(entityData, ESBulletState.ThreadingType.DECOUPLED), new CameraPositionPrinter());
        this.entityData = entityData;
    }

    @Override
    public void simpleInitApp() {
        stateManager.attach(new DebugViewState(entityData, getRootNode()));
        getCamera().setLocation(new Vector3f(17.801697f, 7.7969127f, 29.345581f));
        getCamera().setAxes(new Vector3f(-0.8821641f, 5.6028366E-6f, 0.47094196f),
                new Vector3f(-0.10336524f, 0.9756133f, -0.19363444f),
                new Vector3f(-0.45945835f, -0.21949638f, -0.8606504f));
        //Add some entities
        EntityId plane = entityData.createEntity();
        entityData.setComponents(plane,
                new WarpPosition(new Vector3f(), Quaternion.DIRECTION_Z.clone()),
                new RigidBody(false, 0),
                new CustomShape(new BoxCollisionShape(new Vector3f(250, 0.5f, 250))),
                new Friction(0.65f));

        //rough ramp shouldn't slip here
        EntityId ramp = entityData.createEntity();
        entityData.setComponents(ramp,
                new WarpPosition(new Vector3f(0,1,0), new Quaternion().fromAngleAxis(FastMath.DEG_TO_RAD * 10, Vector3f.UNIT_Z)),
                new RigidBody(false, 0),
                new CustomShape(new BoxCollisionShape(new Vector3f(10, 0.5f, 10))),
                new Friction(0.6f));

        //slippy ramp
        EntityId ramp2 = entityData.createEntity();
        entityData.setComponents(ramp2,
                new WarpPosition(new Vector3f(19.5f,1,0), new Quaternion().fromAngleAxis(FastMath.DEG_TO_RAD * -10, Vector3f.UNIT_Z)),
                new RigidBody(false, 0),
                new CustomShape(new BoxCollisionShape(new Vector3f(10, 0.5f, 10))),
                new Friction(0.1f));

        //some boxes for distance measures
        for(int i = 0; i < 20; i++){
            EntityId box = entityData.createEntity();
            entityData.setComponents(box, new RigidBody(false, 0),
                    new WarpPosition(new Vector3f(-20, 1, -20+2*i), new Quaternion()),
                    new BoxShape());
        }


        character = entityData.createEntity();
        entityData.setComponents(character,
                new PhysicsCharacter(75, 0.5f, 1.8f),
                new PhysicsCharacterMovement(new Vector3f(0,0,0)),
                new WarpPosition(new Vector3f(0, 20, 0), new Quaternion()));

        ESBulletState bulletState = getStateManager().getState(ESBulletState.class);
        bulletState.onInitialize(() -> bulletState.getBulletSystem().addPhysicsSystem(new PhysicsCharacterSystem()));
        bulletState.onInitialize(() -> {
            //Add Debug State to debug physics
            BulletDebugAppState debugAppState = new BulletDebugAppState(bulletState.getPhysicsSpace());
            getStateManager().attach(debugAppState);
            debugAppState.setEnabled(true);
        });

        getInputManager().addMapping("FORWARD", new KeyTrigger(KeyInput.KEY_I));
        getInputManager().addMapping("BACKWARD", new KeyTrigger(KeyInput.KEY_K));
        getInputManager().addMapping("STEP_LEFT", new KeyTrigger(KeyInput.KEY_J));
        getInputManager().addMapping("STEP_RIGHT", new KeyTrigger(KeyInput.KEY_L));
        getInputManager().addListener(this, "FORWARD", "BACKWARD", "STEP_LEFT", "STEP_RIGHT");

        infoText = new BitmapText(guiFont, false);
        infoText.setSize(guiFont.getCharSet().getRenderedSize());      // font size
        infoText.setColor(ColorRGBA.White);                             // font color
        infoText.setText("");             // the text
        infoText.setLocalTranslation(0, infoText.getLineHeight(), 0); // position
        guiNode.attachChild(infoText);

        characterSpeed = entityData.watchEntity(character, LinearVelocity.class);
    }

    @Override
    public void update() {
        super.update();
        characterSpeed.applyChanges();
        infoText.setText("velocity: "+characterSpeed.get(LinearVelocity.class).getVelocity().length());
        infoText.setLocalTranslation(0, infoText.getLineHeight()*infoText.getLineCount(), 0); // position
    }


    private boolean forward = false;
    private boolean backward = false;
    private boolean step_left = false;
    private boolean step_right = false;

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch (name){
            case "FORWARD" : forward = isPressed; break;
            case "BACKWARD" : backward = isPressed; break;
            case "STEP_LEFT" : step_left = isPressed; break;
            case "STEP_RIGHT" : step_right = isPressed; break;
            default: return;
        }
        float move = (forward ? 1.6f : 0) - (backward ? 1.6f : 0); //6km/h = 1.6m/s usual walk speed
        float step = (step_left ? 6 : 0) - (step_right ? 6 : 0); //stepping is faster than walking
        entityData.setComponent(character, new PhysicsCharacterMovement(new Vector3f(step, 0, move)));
    }
}
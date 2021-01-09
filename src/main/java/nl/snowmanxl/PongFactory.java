package nl.snowmanxl;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.entity.components.CollidableComponent;
import com.almasb.fxgl.particle.ParticleComponent;
import com.almasb.fxgl.particle.ParticleEmitter;
import com.almasb.fxgl.particle.ParticleEmitters;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;
import com.almasb.fxgl.physics.PhysicsComponent;
import com.almasb.fxgl.physics.box2d.dynamics.BodyType;
import com.almasb.fxgl.physics.box2d.dynamics.FixtureDef;
import javafx.beans.binding.Bindings;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;
import static com.almasb.fxgl.dsl.FXGL.getip;

public class PongFactory implements EntityFactory {

    @Spawns("bat")
    public Entity newBat(SpawnData data) {
        boolean isPlayer = data.get("isPlayer");

        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.KINEMATIC);

        return entityBuilder()
                .from(data)
                .type(isPlayer ? EntityType.PLAYER_BAT : EntityType.ENEMY_BAT)
                .viewWithBBox(new Rectangle(20, 60, Color.LIGHTGRAY))
                .with(new CollidableComponent(true))
                .with(physics)
                .with(new BatComponent())
                .build();
    }


    @Spawns("clientBall")
    public Entity clientBall(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.STATIC);
        physics.setFixtureDef(new FixtureDef().density(0.3f).restitution(1.0f));

        ParticleEmitter emitter = ParticleEmitters.newFireEmitter();
//        emitter.startColorProperty().bind(
//                Bindings.when(endGame)
//                        .then(Color.GREEN)
//                        .otherwise(Color.LIGHTYELLOW)
//        );
//
//        emitter.endColorProperty().bind(
//                Bindings.when(endGame)
//                        .then(Color.RED)
//                        .otherwise(Color.LIGHTBLUE)
//        );

        emitter.setBlendMode(BlendMode.SRC_OVER);
        emitter.setSize(5, 10);
        emitter.setEmissionRate(1);

        return entityBuilder()
                .from(data)
                .type(EntityType.BALL)
                .bbox(new HitBox(BoundingShape.circle(5)))
                .with(physics)
                .with(new CollidableComponent(true))
                .with(new ParticleComponent(emitter))
                .build();
    }

    @Spawns("ball")
    public Entity newBall(SpawnData data) {
        PhysicsComponent physics = new PhysicsComponent();
        physics.setBodyType(BodyType.DYNAMIC);
        physics.setFixtureDef(new FixtureDef().density(0.3f).restitution(1.0f));
        physics.setOnPhysicsInitialized(() -> physics.setLinearVelocity(5 * 60, -5 * 60));

//        var endGame = getip("player1score").isEqualTo(10).or(getip("player2score").isEqualTo(10));

        ParticleEmitter emitter = ParticleEmitters.newFireEmitter();
//        emitter.startColorProperty().bind(
//                Bindings.when(endGame)
//                        .then(Color.GREEN)
//                        .otherwise(Color.LIGHTYELLOW)
//        );
//
//        emitter.endColorProperty().bind(
//                Bindings.when(endGame)
//                        .then(Color.RED)
//                        .otherwise(Color.LIGHTBLUE)
//        );

        emitter.setBlendMode(BlendMode.SRC_OVER);
        emitter.setSize(5, 10);
        emitter.setEmissionRate(1);

        return entityBuilder()
                .from(data)
                .type(EntityType.BALL)
                .bbox(new HitBox(BoundingShape.circle(5)))
                .with(physics)
                .with(new CollidableComponent(true))
                .with(new ParticleComponent(emitter))
                .with(new BallComponent())
                .build();
    }

}

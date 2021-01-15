package nl.snowmanxl;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.core.serialization.Bundle;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.input.UserAction;
import com.almasb.fxgl.net.Client;
import com.almasb.fxgl.net.Connection;
import com.almasb.fxgl.net.Server;
import com.almasb.fxgl.physics.PhysicsComponent;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;

import java.util.Optional;

import static com.almasb.fxgl.dsl.FXGL.addUINode;
import static com.almasb.fxgl.dsl.FXGL.entityBuilder;
import static com.almasb.fxgl.dsl.FXGL.getAppHeight;
import static com.almasb.fxgl.dsl.FXGL.getAppWidth;
import static com.almasb.fxgl.dsl.FXGL.getDialogService;
import static com.almasb.fxgl.dsl.FXGL.getGameWorld;
import static com.almasb.fxgl.dsl.FXGL.getInput;
import static com.almasb.fxgl.dsl.FXGL.getNetService;
import static com.almasb.fxgl.dsl.FXGL.getPhysicsWorld;
import static com.almasb.fxgl.dsl.FXGL.getUIFactoryService;
import static com.almasb.fxgl.dsl.FXGL.removeUINode;
import static com.almasb.fxgl.dsl.FXGL.runOnce;
import static com.almasb.fxgl.dsl.FXGL.spawn;

public class MainScene extends GameApplication {
    private Server<Bundle> server;
    private Client<Bundle> client;
    private BatComponent bat1;
    private BatComponent bat2;

    private boolean isServer;
    private Button btnNext;
    private Entity clientBall;
    private String connectionAddress;

    private long previousBallFrame;
    private long previousBatFrame;

    @Override
    protected void initSettings(GameSettings gameSettings) {
        gameSettings.setTitle("Pong Multiplayer");
        gameSettings.setVersion("1.0");
    }

    @Override
    protected void initGame() {
        runOnce(() -> getDialogService().showConfirmationBox("Is Server?", this::handleServerAnswer), Duration.seconds(0.01));

        getGameWorld().addEntityFactory(new PongFactory());
        initScreenBounds();
        initGameObjects();
    }

    private void startupClient() {
        client = getNetService().newTCPClient(connectionAddress, 55555);
        client.setOnConnected(connection -> {


            connection.addMessageHandlerFX((Connection<Bundle> conn, Bundle message) -> {

                if (message.getName().equals("Bat1Position")) {
                    handleBatPosition(message, bat1);
                    if(previousBatFrame > 0) {
                        var current = System.currentTimeMillis();
                        var diff = current - previousBatFrame;
                        System.out.println( "frametime: " + diff + " | BAT_FRAME");
                        previousBatFrame = current;
                    }
                } else if (message.getName().equals("GameUpdate")) {
                    if (message.getData().get("event").equals("start")) {
                        spawnClientBall();
                    }
                } else if (message.getName().equals("BallPosition")) {
                    var x = (double) message.getData().get("x");
                    var y = (double) message.getData().get("y");

                    if(previousBallFrame > 0) {
                        var current = System.currentTimeMillis();
                        var diff = current - previousBallFrame;
                        System.out.println( "frametime: " + diff + " | BALL_FRAME");
                        previousBallFrame = current;
                    }
                    Optional.ofNullable(clientBall).ifPresent(ball -> {
                        ball.getComponent(PhysicsComponent.class).overwritePosition(new Point2D(x, y));
                    });
                }
            });
        });
        client.connectAsync();
    }

    private void startupServer() {
        server = getNetService().newTCPServer(55555);
        server.setOnConnected(connection -> {
            connection.addMessageHandlerFX((conn, message) -> {
                if (message.getName().equals("Bat2Position")) {
                    handleBatPosition(message, bat2);
                }
            });
        });
        server.startAsync();
    }

    private void handleBatPosition(Bundle message, BatComponent bat) {
        var x = (double) message.getData().get("x");
        var y = (double) message.getData().get("y");
        bat.getEntity().getComponent(PhysicsComponent.class).overwritePosition(new Point2D(x, y));
    }

    @Override
    protected void initInput() {
        getInput().addAction(new UserAction("Up") {
            @Override
            protected void onAction() {
                if (isServer) {
                    bat1.up();
                    broadCastBat1Position("Up");
                } else {
                    bat2.up();
                    broadCastBat2Position("Up");
                }
            }

            @Override
            protected void onActionEnd() {
                if (isServer) {
                    bat1.stop();
                    broadCastBat1Position("stop");
                } else {
                    bat2.stop();
                    broadCastBat2Position("stop");
                }
            }
        }, KeyCode.W);

        getInput().addAction(new UserAction("Down") {
            @Override
            protected void onAction() {
                if (isServer) {
                    bat1.down();
                    broadCastBat1Position("Down");
                } else {
                    bat2.down();
                    broadCastBat2Position("Down");
                }
            }

            @Override
            protected void onActionEnd() {
                if (isServer) {
                    bat1.stop();
                    broadCastBat1Position("stop");
                } else {
                    bat2.stop();
                    broadCastBat2Position("stop");
                }
            }
        }, KeyCode.S);
    }

    @Override
    protected void initPhysics() {
        getPhysicsWorld().setGravity(0, 0);
    }

    protected void setStartButton() {
        btnNext = getUIFactoryService().newButton("Start Game");
        btnNext.setOnAction(e -> startGame());

        addUINode(btnNext, getAppWidth() - 250, getAppHeight() - 40);
    }

    private void startGame() {
        spawnBall();
        broadCastStartGame();
        removeUINode(btnNext);
    }

    private void handleServerAnswer(Boolean answer) {
        isServer = answer;
        if (isServer) {
            setStartButton();
            startupServer();
        } else {
            getDialogService().showInputBox("Connect to server:", serverAddress -> {
                connectionAddress = serverAddress;
                startupClient();
            });
        }

    }

    private void spawnBall() {
        Entity ball = spawn("ball", getAppWidth() / 2 - 5, getAppHeight() / 2 - 5);
        var component = ball.getComponent(BallComponent.class);
        component.addUpdateListener(this::serverBroadcastBallPosition);
    }

    private void spawnClientBall() {
        clientBall = spawn("clientBall", getAppWidth() / 2 - 5, getAppHeight() / 2 - 5);
    }

    private void initGameObjects() {
//
        Entity bat1 = spawn("bat", new SpawnData(getAppWidth() / 4, getAppHeight() / 2 - 30).put("isPlayer", true));
        Entity bat2 = spawn("bat", new SpawnData(3 * getAppWidth() / 4 - 20, getAppHeight() / 2 - 30)
                .put("isPlayer", false));

        this.bat1 = bat1.getComponent(BatComponent.class);
        this.bat2 = bat2.getComponent(BatComponent.class);
    }

    private void initScreenBounds() {
        Entity walls = entityBuilder()
                .type(EntityType.WALL)
                .collidable()
                .buildScreenBounds(150);

        getGameWorld().addEntity(walls);
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void broadCastBat1Position(String direction) {
        var bundle = new Bundle("Bat1Position");
        var position = bat1.getEntity().getPosition();
        bundle.put("x", position.getX());
        bundle.put("y", position.getY());
        bundle.put("direction", direction);
        server.broadcast(bundle);
    }

    private void broadCastStartGame() {
        var bundle = new Bundle("GameUpdate");
        bundle.put("event", "start");
        server.broadcast(bundle);
    }

    private void broadCastBat2Position(String direction) {
        var bundle = new Bundle("Bat2Position");
        var position = bat2.getEntity().getPosition();
        bundle.put("x", position.getX());
        bundle.put("y", position.getY());
        bundle.put("direction", direction);
        client.broadcast(bundle);
    }

    private void serverBroadcastBallPosition(double x, double y) {
        var bundle = new Bundle("BallPosition");
        bundle.put("x", x);
        bundle.put("y", y);
        server.broadcast(bundle);
    }
}

package sg.quarkus.amqp091.protocol.frame;

public class AMQPPayloadHeaderType {

    public enum Connection implements IFramePayloadClass, IFramePayloadMethod {
        START(10),
        START_OK(11),
        SECURE(20),
        SECURE_OK(21),
        TUNE(30),
        TUNE_OK(31),
        OPEN(40),
        OPEN_OK(41),
        CLOSE(50),
        CLOSE_OK(51);

        private final short id;

        Connection(int id) {
            this.id = (short) id;
        }

        @Override
        public int getMethodId() {
            return id;
        }

        @Override
        public int getClassId() {
            return 10;
        }

        @Override
        public String toString() {
            return this.name() + "(" + this.id + ")";
        }
    }

    public enum Flow implements IFramePayloadClass, IFramePayloadMethod {
        FLOW(20),
        FLOW_OK(21),
        CLOSE(40),
        CLOSE_OK(41),
        OPEN(10),
        OPEN_OK(11); // From channel class

        private final short id;

        Flow(int id) {
            this.id = (short) id;
        }

        @Override
        public int getMethodId() {
            return id;
        }

        @Override
        public String toString() {
            return this.name() + "(" + this.id + ")";
        }

        @Override
        public int getClassId() {
            return 20;
        }
    }

    public enum Exchange implements IFramePayloadClass, IFramePayloadMethod {
        DECLARE(10),
        DECLARE_OK(11),
        DELETE(20),
        DELETE_OK(21);

        private final short id;

        Exchange(int id) {
            this.id = (short) id;
        }

        @Override
        public int getMethodId() {
            return id;
        }

        @Override
        public String toString() {
            return this.name() + "(" + this.id + ")";
        }

        @Override
        public int getClassId() {
            return 40;
        }
    }

    public enum Queue implements IFramePayloadClass, IFramePayloadMethod {
        DECLARE(10),
        DECLARE_OK(11),
        BIND(20),
        BIND_OK(21),
        UNBIND(50),
        UNBIND_OK(51),
        PURGE(30),
        PURGE_OK(31),
        DELETE(40),
        DELETE_OK(41);

        private final short id;

        Queue(int id) {
            this.id = (short) id;
        }

        @Override
        public int getMethodId() {
            return id;
        }

        @Override
        public String toString() {
            return this.name() + "(" + this.id + ")";
        }

        @Override
        public int getClassId() {
            return 50;
        }
    }

    public enum Basic implements IFramePayloadClass, IFramePayloadMethod, ICanHaveContent {
        QOS(10, false),
        QOS_OK(11, false),
        CONSUME(20, false),
        CONSUME_OK(21, false),
        CANCEL(30, false),
        CANCEL_OK(31, false),
        PUBLISH(40, true),
        RETURN(50, true),
        DELIVER(60, true),
        GET(70, false),
        GET_OK(71, true),
        GET_EMPTY(72, false), // ALERT - is only self contained if a message is retrieved
        ACK(80, false),
        REJECT(90, false),
        RECOVER_ASYNC(100, false),
        RECOVER(110, false),
        RECOVER_OK(111, false);

        private final short id;
        private final boolean hasContent;

        Basic(int id, boolean hasContent) {
            this.id = (short) id;
            this.hasContent = hasContent;
        }

        @Override
        public int getMethodId() {
            return id;
        }

        @Override
        public String toString() {
            return this.name() + "(" + this.id + ")";
        }

        @Override
        public int getClassId() {
            return 60;
        }

        @Override
        public boolean hasContent() {
            return hasContent;
        }
    }

    public enum Tx implements IFramePayloadClass, IFramePayloadMethod {
        SELECT(10),
        SELECT_OK(11),
        COMMIT(20),
        COMMIT_OK(21),
        ROLLBACK(30),
        ROLLBACK_OK(31);

        private final short id;

        Tx(int id) {
            this.id = (short) id;
        }

        @Override
        public int getMethodId() {
            return id;
        }

        @Override
        public String toString() {
            return this.name() + "(" + this.id + ")";
        }

        @Override
        public int getClassId() {
            return 90;
        }
    }
}
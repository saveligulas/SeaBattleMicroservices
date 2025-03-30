package sg.quarkus.amqp091.protocol.frame;

public class AMQPPayloadHeader implements IAMQPPayloadHeader {
    private final short classId;
    private final short methodId;

    public AMQPPayloadHeader(short classId, short methodId) {
        this.classId = classId;
        this.methodId = methodId;
    }

    @Override
    public int getClassId() {
        return classId;
    }

    @Override
    public int getMethodId() {
        return methodId;
    }

    public enum Connection implements IAMQPPayloadHeader {
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

    public enum Flow implements IAMQPPayloadHeader {
        FLOW(20),
        FLOW_OK(21),
        CLOSE(40),
        CLOSE_OK(41),
        OPEN(10), // From channel class
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

    public enum Exchange implements IAMQPPayloadHeader {
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

    public enum Queue implements IAMQPPayloadHeader {
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

    public enum Basic implements IAMQPPayloadHeader {
        QOS(10),
        QOS_OK(11),
        CONSUME(20),
        CONSUME_OK(21),
        CANCEL(30),
        CANCEL_OK(31),
        PUBLISH(40),
        RETURN(50),
        DELIVER(60),
        GET(70),
        GET_OK(71),
        GET_EMPTY(72),
        ACK(80),
        REJECT(90),
        RECOVER_ASYNC(100),
        RECOVER(110),
        RECOVER_OK(111);

        private final short id;

        Basic(int id) {
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
            return 60;
        }
    }

    public enum Tx implements IAMQPPayloadHeader {
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
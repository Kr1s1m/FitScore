
create table accounts(
    account_id uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    account_date_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    account_email character varying(255),
    account_username character varying(25),
    account_password character varying(255),
    account_birth_date date,
    account_height smallint,
    account_weight numeric(4,1)
);

create table posts(
    post_id uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    account_id uuid  NOT NULL REFERENCES accounts(account_id),
    post_date_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    post_date_updated timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    post_title character varying(100),
    post_body text
    post_vote_balance integer NOT NULL DEFAULT 0
);

create table replies(
    reply_id uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    account_id uuid NOT NULL REFERENCES accounts(account_id),
    post_id uuid NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
    reply_parent_id uuid REFERENCES replies(reply_id) ON DELETE CASCADE,
    reply_date_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    reply_date_updated timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    reply_body text
    reply_vote_balance integer NOT NULL DEFAULT 0
);

create table verification_tokens(
    verification_token_id uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    account_id uuid NOT NULL REFERENCES accounts(account_id),
    verification_token_expiration_date timestamp without time zone,
    verification_token_token character varying(255)
);

create table votes(
    vote_id uuid PRIMARY KEY NOT NULL DEFAULT gen_random_uuid (),
    account_id uuid NOT NULL REFERENCES accounts(account_id),
    post_id uuid NOT NULL REFERENCES posts(post_id),
    reply_id uuid REFERENCES replies(reply_id),
    vote_type character varying(255) NOT NULL DEFAULT 'upvote',
    vote_target character varying(255) NOT NULL DEFAULT 'post'
);

create table roles(
    role_id uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    role_access_type character varying(255) NOT NULL DEFAULT 'user'
);

create table accounts_roles(
    PRIMARY KEY(account_id, role_id),
    account_id uuid REFERENCES accounts(account_id),
    role_id uuid REFERENCES roles(role_id)
);

-- Inserts:
insert into roles(
	role_access_type
) values (
	'user'
);

insert into roles(
	role_access_type
) values (
	'admin'
);

insert into accounts(
	account_email,
	account_username,
	account_password,
	account_birth_date,
	account_height,
	account_weight
) values (
	'dero@fitscore.com',
	'Dero',
	'(@*#N#Y#(@!)H#$H(DN($$(&*!JD8',
	'1999-03-25',
	164,
	67.4
);

insert into accounts(
	account_email,
	account_username,
	account_password,
	account_birth_date,
	account_height,
	account_weight
) values (
	'kr1s1m@fitscore.com',
	'Kr1s1m',
	'&#$^DHR$&(M8d4j(!#&!!3893jd8',
	'1999-01-11',
	173,
	54.5
);

INSERT INTO accounts_roles(account_id, role_id)
VALUES
((SELECT account_id FROM accounts WHERE account_username = 'Dero'), (SELECT role_id FROM roles WHERE role_access_type = 'admin')),
((SELECT account_id FROM accounts WHERE account_username = 'Dero'), (SELECT role_id FROM roles WHERE role_access_type = 'user')),
((SELECT account_id FROM accounts WHERE account_username = 'Kr1s1m'), (SELECT role_id FROM roles WHERE role_access_type = 'user')),
((SELECT account_id FROM accounts WHERE account_username = 'Kr1s1m'), (SELECT role_id FROM roles WHERE role_access_type = 'admin'));

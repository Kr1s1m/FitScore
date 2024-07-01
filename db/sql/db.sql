
create table accounts(
    account_id uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    account_date_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    account_email character varying(255),
    account_username character varying(25),
    account_password character varying(255),
    account_age smallint,
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
);

create table replies(
    reply_id uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    account_id uuid NOT NULL REFERENCES accounts(account_id),
    post_id uuid NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
    reply_parent_id uuid REFERENCES replies(reply_id) ON DELETE CASCADE,
    reply_date_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    reply_date_updated timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    reply_body text
);

create table verification_tokens(
    verification_token_id uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    account_id uuid NOT NULL REFERENCES accounts(account_id),
    verification_token_expiration_date timestamp without time zone,
    verification_token_token character varying(255)
);

create type vote_type as enum ('up', 'down');

create table post_votes(
    post_vote_id uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    account_id uuid NOT NULL REFERENCES accounts(account_id),
    post_id uuid NOT NULL REFERENCES posts(post_id),
    post_vote_vote_type vote_type
);

create table reply_votes(
    reply_vote_id uuid primary key NOT NULL DEFAULT gen_random_uuid (),
    account_id uuid NOT NULL REFERENCES accounts(account_id),
    reply_id uuid NOT NULL REFERENCES posts(post_id),
    reply_vote_vote_type vote_type
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
	account_age,
	account_height,
	account_weight
) values (
	'dero@fitscore.com',
	'Dero',
	25,
	164,
	67.4
);

insert into accounts(
	account_email,
	account_username,
	account_age,
	account_height,
	account_weight
) values (
	'kr1s1m@fitscore.com',
	'Kr1s1m',
	25,
	173,
	54.5
);

INSERT INTO accounts_roles(account_id, role_id)
VALUES
((SELECT account_id FROM accounts WHERE account_username = 'Dero'), (SELECT role_id FROM roles WHERE role_access_type = 'admin')),
((SELECT account_id FROM accounts WHERE account_username = 'Dero'), (SELECT role_id FROM roles WHERE role_access_type = 'user')),
((SELECT account_id FROM accounts WHERE account_username = 'Kr1s1m'), (SELECT role_id FROM roles WHERE role_access_type = 'user')),
((SELECT account_id FROM accounts WHERE account_username = 'Kr1s1m'), (SELECT role_id FROM roles WHERE role_access_type = 'admin'));